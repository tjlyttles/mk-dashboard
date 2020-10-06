package com.mkdecision.dashboard

import org.moqui.context.ExecutionContext
import org.moqui.context.L10nFacade
import org.moqui.context.MessageFacade
import org.moqui.context.UserFacade
import org.moqui.entity.EntityCondition
import org.moqui.entity.EntityConditionFactory
import org.moqui.entity.EntityFacade
import org.moqui.entity.EntityList
import org.moqui.entity.EntityValue
import org.moqui.impl.entity.EntityFacadeImpl
import org.moqui.service.ServiceFacade
import org.moqui.util.ContextStack

class OrderServices {

    static Map<String, Object> validateOrderFields(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        UserFacade uf = ec.getUser()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String salesChannelEnumId = (String) cs.getOrDefault("salesChannelEnumId", null)
        String productStoreId = (String) cs.getOrDefault("productStoreId", null)
        String salesRepresentativeId = (String) cs.getOrDefault("salesRepresentativeId", null)
        String productCategoryId = (String) cs.getOrDefault("productCategoryId", null)
        String productId = (String) cs.getOrDefault("productId", null)
        Double totalPurchasePrice = (Double) cs.getOrDefault("totalPurchasePrice", null)
        Double downPayment = (Double) cs.getOrDefault("downPayment", null)
        Double loanFee = (Double) cs.getOrDefault("loanFee", null)
        String amount = (String) cs.getOrDefault("amount", null)
        Double projectedAmount = (Double) cs.getOrDefault("projectedAmount", null)

        // validate product store
        EntityFacadeImpl efi = (EntityFacadeImpl) ef
        EntityConditionFactory ecf = efi.getConditionFactory()
        long storeCount = ef.find("mantle.product.store.ProductStoreParty")
                .condition("productStoreId", productStoreId)
                .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                .condition(
                        ecf.makeCondition(
                                ecf.makeCondition("roleTypeId", EntityCondition.ComparisonOperator.EQUALS, "FinanceManager"),
                                EntityCondition.JoinOperator.OR,
                                ecf.makeCondition("roleTypeId", EntityCondition.ComparisonOperator.EQUALS, "SalesRepresentative")
                        )
                )
                .count()
        if (storeCount == 0) {
            mf.addError(lf.localize("ORD_INVALID_PRODUCT_STORE"))
            return new HashMap<String, Object>()
        }

        // validate sales representative
        boolean isFinanceManager = uf.getUserGroupIdSet().contains("FinanceManager")
        String userPartyId = uf.getUserAccount().getString("partyId")
        if (isFinanceManager) {
            long salesRepCount = ef.find("mantle.product.store.ProductStoreParty")
                    .condition("productStoreId", productStoreId)
                    .condition("partyId", salesRepresentativeId)
                    .condition("roleTypeId", "SalesRepresentative")
                    .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                    .count()
            if (salesRepCount == 0) {
                mf.addError(lf.localize("ORD_INVALID_SALES_REP"))
                return new HashMap<String, Object>()
            }
        } else if (salesRepresentativeId != userPartyId) {
            mf.addError(lf.localize("ORD_INVALID_SALES_REP"))
            return new HashMap<String, Object>()
        }

        // validate product category
        long productCategoryCount = ef.find("mkdecision.dashboard.ProductStoreCategoryDetail")
                .condition("productStoreId", productStoreId)
                .condition("storeCategoryTypeEnumId", "PsctFinanceableProducts")
                .condition("productCategoryId", productCategoryId)
                .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                .count()
        if (productCategoryCount == 0) {
            mf.addError(lf.localize("ORD_INVALID_PRODUCT_CATEGORY"))
            return new HashMap<String, Object>()
        }

        // validate product category member
        long categoryMemberCount = ef.find("mantle.product.category.ProductCategoryMember")
                .condition("productCategoryId", productCategoryId)
                .condition("productId", productId)
                .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                .count()
        if (categoryMemberCount == 0) {
            mf.addError(lf.localize("ORD_INVALID_PRODUCT"))
            return new HashMap<String, Object>()
        }

        // validate product
        long productCount = ef.find("mkdecision.dashboard.ProductStoreProductDetail")
                .condition("productStoreId", productStoreId)
                .condition("productId", productId)
                .count()
        if (productCount == 0) {
            mf.addError(lf.localize("ORD_INVALID_PRODUCT"))
            return new HashMap<String, Object>()
        }

        // validate total purchase price
        if (totalPurchasePrice == null || totalPurchasePrice <= 0) {
            mf.addError(lf.localize("ORD_INVALID_TOTAL_PURCHASE_PRICE"))
            return new HashMap<String, Object>()
        }

        // validate down payment
        if (downPayment == null || downPayment <= 0 || downPayment > totalPurchasePrice) {
            mf.addError(lf.localize("ORD_INVALID_DOWN_PAYMENT"))
            return new HashMap<String, Object>()
        }

        // validate loan fee
        if (loanFee == null || loanFee <= 0) {
            mf.addError(lf.localize("ORD_INVALID_LOAN_FEE"))
            return new HashMap<String, Object>()
        }

        // validate amount
        Double amountDouble;
        try {
            amountDouble = Double.parseDouble(amount)
        } catch (NumberFormatException e) {
        }
        if (amountDouble == null || amountDouble != ((totalPurchasePrice + loanFee) - downPayment)) {
            mf.addError(lf.localize("ORD_INVALID_AMOUNT"))
            return new HashMap<String, Object>()
        }

        // validate project amount
        if (projectedAmount == null || projectedAmount <= 0) {
            mf.addError(lf.localize("ORD_INVALID_PROJECTED_AMOUNT"))
            return new HashMap<String, Object>()
        }

        // return the output parameters
        return new HashMap<>()
    }

    static Map<String, Object> createOrder(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        ServiceFacade sf = ec.getService()
        UserFacade uf = ec.getUser()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String salesChannelEnumId = (String) cs.getOrDefault("salesChannelEnumId", null)
        String productStoreId = (String) cs.getOrDefault("productStoreId", null)
        String salesRepresentativeId = (String) cs.getOrDefault("salesRepresentativeId", null)
        String productCategoryId = (String) cs.getOrDefault("productCategoryId", null)
        String productId = (String) cs.getOrDefault("productId", null)
        Double totalPurchasePrice = (Double) cs.getOrDefault("totalPurchasePrice", null)
        Double downPayment = (Double) cs.getOrDefault("downPayment", null)
        Double loanFee = (Double) cs.getOrDefault("loanFee", null)
        Double amount = (Double) cs.getOrDefault("amount", null)
        Double projectedAmount = (Double) cs.getOrDefault("projectedAmount", null)

        // validate order fields
        sf.sync().name("mkdecision.dashboard.OrderServices.validate#OrderFields")
                .parameters(cs)
                .call();
        if (mf.hasError()) {
            return new HashMap<String, Object>();
        }

        // find product form
        EntityList formList = ef.find("mantle.product.ProductDbForm")
                .condition("productId", productId)
                .list();
        EntityValue form = formList.isEmpty() ? null : formList.getFirst();
        if (form == null) {
            mf.addError(lf.localize("ORD_INVALID_ELIGIBILITY_FORM"))
            return new HashMap<String, Object>()
        }

        // add eligibility answers to map
        Map<String, String> answerMap = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            String fieldName = String.format("eligibilityQuestion%d", i);
            answerMap.put(fieldName, (String) cs.getOrDefault(fieldName, null))
        }

        // validate form fields
        EntityList fieldList = ef.find("moqui.screen.form.DbFormField")
                .condition("formId", form.getString("formId"))
                .list()
        for (EntityValue field : fieldList) {
            String fieldName = field.getString("fieldName")
            String answer = answerMap.get(fieldName);
            if (answer == null || answer != "true") {
                mf.addError(lf.localize("ORD_APPLICANT_NOT_ELIGIBLE"))
                return new HashMap<String, Object>()
            }
        }

        // create order header
        Map<String, Object> orderHeaderResp = sf.sync().name("mantle.order.OrderServices.create#Order")
                .parameter("salesChannelEnumId", salesChannelEnumId)
                .parameter("enteredByPartyId", uf.getUserAccount().getString("partyId"))
                .parameter("productStoreId", productStoreId)
                .call()
        String orderId = (String) orderHeaderResp.get("orderId")
        String orderPartSeqId = (String) orderHeaderResp.get("orderPartSeqId")

        // create order party
        sf.sync().name("create#mantle.order.OrderPartParty")
                .parameter("orderId", orderId)
                .parameter("orderPartSeqId", orderPartSeqId)
                .parameter("partyId", salesRepresentativeId)
                .parameter("roleTypeId", "SalesRepresentative")
                .call()

        // create product parameter set
        Map<String, Object> productParameterSetResp = sf.sync().name("create#mantle.product.ProductParameterSet")
                .parameter("productId", productId)
                .call()
        String productParameterSetId = (String) productParameterSetResp.get("productParameterSetId")

        // create order item
        sf.sync().name("mantle.order.OrderServices.create#OrderItem")
                .parameter("orderId", orderId)
                .parameter("orderPartSeqId", orderPartSeqId)
                .parameter("productId", productId)
                .parameter("productParameterSetId", productParameterSetId)
                .parameter("unitAmount", amount)
                .call();

        // create product category
        sf.sync().name("create#mantle.product.ProductParameterValue")
            .parameter("productParameterId", "ProductCategory")
            .parameter("productParameterSetId", productParameterSetId)
            .parameter("parameterValue", productCategoryId)
            .call()

        // create purchase price
        sf.sync().name("create#mantle.product.ProductParameterValue")
                .parameter("productParameterId", "TotalPurchasePrice")
                .parameter("productParameterSetId", productParameterSetId)
                .parameter("parameterValue", totalPurchasePrice)
                .call()

        // create down payment
        sf.sync().name("create#mantle.product.ProductParameterValue")
                .parameter("productParameterId", "DownPayment")
                .parameter("productParameterSetId", productParameterSetId)
                .parameter("parameterValue", downPayment)
                .call()

        // create loan fee
        sf.sync().name("create#mantle.product.ProductParameterValue")
            .parameter("productParameterId", "LoanFee")
            .parameter("productParameterSetId", productParameterSetId)
            .parameter("parameterValue", loanFee)
            .call()

        // create projected payment
        sf.sync().name("create#mantle.product.ProductParameterValue")
            .parameter("productParameterId", "ProjectedPayment")
            .parameter("productParameterSetId", productParameterSetId)
            .parameter("parameterValue", projectedAmount)
            .call()

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("orderId", orderId)
        outParams.put("orderPartSeqId", orderPartSeqId)
        return outParams
    }
}
