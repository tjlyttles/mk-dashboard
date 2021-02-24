package com.mkdecision.dashboard

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.DateUtils
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

@SuppressWarnings("unused")
class OrderServices {

    static void validateOrderAccess(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        UserFacade uf = ec.getUser()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String orderId = (String) cs.getOrDefault("orderId", null)
        String partyId = uf.getUserAccount().getString("partyId")

        // validate order
        EntityFacadeImpl efi = (EntityFacadeImpl) ef
        EntityConditionFactory ecf = efi.getConditionFactory()
        long orderCount = ef.find("mkdecision.dashboard.OrderHeaderDetail")
                .condition(ecf.makeCondition(
                        ecf.makeCondition("orderId", EntityCondition.ComparisonOperator.EQUALS, orderId),
                        EntityCondition.JoinOperator.AND,
                        ecf.makeCondition(
                                Arrays.asList(
                                        ecf.makeCondition(
                                                Arrays.asList(
                                                        ecf.makeCondition("orderPartPartyId", EntityCondition.ComparisonOperator.EQUALS, partyId),
                                                        ecf.makeCondition("orderPartRoleTypeId", EntityCondition.ComparisonOperator.EQUALS, "SalesRepresentative")
                                                ),
                                                EntityCondition.JoinOperator.AND
                                        ),
                                        ecf.makeCondition(
                                                Arrays.asList(
                                                        ecf.makeCondition("storePartyId", EntityCondition.ComparisonOperator.EQUALS, partyId),
                                                        ecf.makeCondition("storeRoleTypeId", EntityCondition.ComparisonOperator.EQUALS, "SalesRepresentative"),
                                                        ecf.makeCondition("storeAllowSalesRepViewAll", EntityCondition.ComparisonOperator.EQUALS, "Y"),
                                                ),
                                                EntityCondition.JoinOperator.AND
                                        ),
                                        ecf.makeCondition(
                                                Arrays.asList(
                                                        ecf.makeCondition("storePartyId", EntityCondition.ComparisonOperator.EQUALS, partyId),
                                                        ecf.makeCondition("storeRoleTypeId", EntityCondition.ComparisonOperator.EQUALS, "FinanceManager")
                                                ),
                                                EntityCondition.JoinOperator.AND
                                        ),
                                        ecf.makeCondition(
                                                Arrays.asList(
                                                        ecf.makeCondition("vendorRelationshipTypeId", EntityCondition.ComparisonOperator.EQUALS, "PrtEmployee"),
                                                        ecf.makeCondition("vendorRelationshipFromPartyId", EntityCondition.ComparisonOperator.EQUALS, partyId),
                                                        ecf.makeCondition("vendorRelationshipFromRoleTypeId", EntityCondition.ComparisonOperator.EQUALS, "Underwriter")
                                                ),
                                                EntityCondition.JoinOperator.AND
                                        )
                                ),
                                EntityCondition.JoinOperator.OR
                        )
                ))
                .count()
        if (orderCount == 0) {
            mf.addError(lf.localize("DASHBOARD_ORDER_ACCESS_DENIED"))
        }
    }

    static void validateOrderFields(ExecutionContext ec) {

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
        BigDecimal totalPurchaseAmount = (BigDecimal) cs.getOrDefault("totalPurchaseAmount", null)
        BigDecimal downPayment = (BigDecimal) cs.getOrDefault("downPayment", null)
        BigDecimal netPurchaseAmount = (BigDecimal) cs.getOrDefault("netPurchaseAmount", null)
        BigDecimal loanFee = (BigDecimal) cs.getOrDefault("loanFee", null)
        String financedAmount = (String) cs.getOrDefault("financedAmount", null)
        BigDecimal estimatedPayment = (BigDecimal) cs.getOrDefault("estimatedPayment", null)

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
            mf.addError(lf.localize("DASHBOARD_INVALID_PRODUCT_STORE"))
            return
        }

        // validate sales representative
        String userPartyId = uf.getUserAccount().getString("partyId")
        long financeManagerCount = ef.find("mantle.product.store.ProductStoreParty")
                .condition("productStoreId", productStoreId)
                .condition("partyId", userPartyId)
                .condition("roleTypeId", "FinanceManager")
                .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                .count()
        if (financeManagerCount > 0) {
            long salesRepCount = ef.find("mantle.product.store.ProductStoreParty")
                    .condition("productStoreId", productStoreId)
                    .condition("partyId", salesRepresentativeId)
                    .condition("roleTypeId", "SalesRepresentative")
                    .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                    .count()
            if (salesRepCount == 0) {
                mf.addError(lf.localize("DASHBOARD_INVALID_SALES_REP"))
                return
            }
        } else if (salesRepresentativeId != userPartyId) {
            mf.addError(lf.localize("DASHBOARD_INVALID_SALES_REP"))
            return
        }

        // validate product category
        long productCategoryCount = ef.find("mkdecision.dashboard.ProductStoreCategoryDetail")
                .condition("productStoreId", productStoreId)
                .condition("storeCategoryTypeEnumId", "PsctFinanceableProducts")
                .condition("productCategoryId", productCategoryId)
                .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                .count()
        if (productCategoryCount == 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_PRODUCT_CATEGORY"))
            return
        }

        // validate product category member
        long categoryMemberCount = ef.find("mantle.product.category.ProductCategoryMember")
                .condition("productCategoryId", productCategoryId)
                .condition("productId", productId)
                .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                .count()
        if (categoryMemberCount == 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_PRODUCT"))
            return
        }

        // validate total purchase amount
        if (totalPurchaseAmount == null || totalPurchaseAmount <= 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_TOTAL_PURCHASE_AMOUNT"))
            return
        }

        // validate down payment
        if (downPayment == null || downPayment < 0 || downPayment > totalPurchaseAmount) {
            mf.addError(lf.localize("DASHBOARD_INVALID_DOWN_PAYMENT"))
            return
        }

        // validate net purchase amount
        if (netPurchaseAmount == null || netPurchaseAmount != (totalPurchaseAmount - downPayment)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_NET_PURCHASE_AMOUNT"))
            return
        }

        // validate loan fee
        if (loanFee == null || loanFee < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_LOAN_FEE"))
            return
        }

        // validate financed amount
        BigDecimal financedAmountBigDecimal = new BigDecimal(financedAmount)
        if (financedAmountBigDecimal == null || financedAmountBigDecimal != ((totalPurchaseAmount + loanFee) - downPayment)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_FINANCED_AMOUNT"))
            return
        }

        // validate estimated amount
        if (estimatedPayment == null || estimatedPayment <= 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ESTIMATED_AMOUNT"))
        }
    }

    static void validateFormResponse(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        ServiceFacade sf = ec.getService()
        UserFacade uf = ec.getUser()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String productId = (String) cs.getOrDefault("productId", null)
        String formResponseId = (String) cs.getOrDefault("formResponseId", null)

        // find product form
        EntityList formList = ef.find("mantle.product.ProductDbForm")
                .condition("productId", productId)
                .list()
        EntityValue form = formList.isEmpty() ? null : formList.getFirst()
        if (form == null) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ELIGIBILITY_FORM"))
            return
        }

        // validate form fields
        String formId = form.getString("formId")
        EntityList fieldList = ef.find("moqui.screen.form.DbFormField")
                .condition("formId", formId)
                .list()
        for (EntityValue field : fieldList) {
            long answerCount = ef.find("moqui.screen.form.FormResponseAnswer")
                    .condition("formResponseId", formResponseId)
                    .condition("formId", formId)
                    .condition("fieldName", field.getString("fieldName"))
                    .condition("valueText", "true")
                    .count()
            if (answerCount == 0) {
                mf.addError(lf.localize("DASHBOARD_APPLICANT_NOT_ELIGIBLE"))
                return
            }
        }
    }

    static Map<String, Object> storeOrder(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        ServiceFacade sf = ec.getService()
        UserFacade uf = ec.getUser()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String orderId = (String) cs.getOrDefault("orderId", null)
        String orderPartSeqId = (String) cs.getOrDefault("orderPartSeqId", null)
        String salesChannelEnumId = (String) cs.getOrDefault("salesChannelEnumId", null)
        String productStoreId = (String) cs.getOrDefault("productStoreId", null)
        String salesRepresentativeId = (String) cs.getOrDefault("salesRepresentativeId", null)
        String productCategoryId = (String) cs.getOrDefault("productCategoryId", null)
        String productId = (String) cs.getOrDefault("productId", null)
        String formResponseId = (String) cs.getOrDefault("formResponseId", null)
        BigDecimal totalPurchaseAmount = (BigDecimal) cs.getOrDefault("totalPurchaseAmount", null)
        BigDecimal downPayment = (BigDecimal) cs.getOrDefault("downPayment", null)
        BigDecimal netPurchaseAmount = (BigDecimal) cs.getOrDefault("netPurchaseAmount", null)
        BigDecimal loanFee = (BigDecimal) cs.getOrDefault("loanFee", null)
        BigDecimal financedAmount = (BigDecimal) cs.getOrDefault("financedAmount", null)
        BigDecimal estimatedPayment = (BigDecimal) cs.getOrDefault("estimatedPayment", null)

        // validate fields
        sf.sync().name("mkdecision.dashboard.OrderServices.validate#OrderFields")
                .parameters(cs)
                .call()
        if (mf.hasError()) {
            return new HashMap<String, Object>()
        }

        // store order
        boolean updateOrder = StringUtils.isNotBlank(orderId) && StringUtils.isNotBlank(orderPartSeqId)
        if (updateOrder) {

            // update order header
            sf.sync().name("mantle.order.OrderServices.update#OrderHeader")
                    .parameter("orderId", orderId)
                    .parameter("salesChannelEnumId", salesChannelEnumId)
                    .parameter("productStoreId", productStoreId)
                    .call()

            // delete order party
            sf.sync().name("delete#mantle.order.OrderPartParty")
                    .parameter("orderId", orderId)
                    .parameter("orderPartSeqId", orderPartSeqId)
                    .parameter("partyId", "*")
                    .parameter("roleTypeId", "SalesRepresentative")
                    .call()

            // create order party
            sf.sync().name("create#mantle.order.OrderPartParty")
                    .parameter("orderId", orderId)
                    .parameter("orderPartSeqId", orderPartSeqId)
                    .parameter("partyId", salesRepresentativeId)
                    .parameter("roleTypeId", "SalesRepresentative")
                    .call()

            // find order item
            EntityValue orderItem = ef.find("mantle.order.OrderItem")
                    .condition("orderId", orderId)
                    .condition("orderPartSeqId", orderPartSeqId)
                    .orderBy("-lastUpdatedStamp")
                    .list()
                    .getFirst()
            String orderItemSeqId = orderItem.getString("orderItemSeqId")
            String productParameterSetId = orderItem.getString("productParameterSetId")

            // update order item
            sf.sync().name("mantle.order.OrderServices.update#OrderItem")
                    .parameter("orderId", orderId)
                    .parameter("orderPartSeqId", orderPartSeqId)
                    .parameter("orderItemSeqId", orderItemSeqId)
                    .parameter("productId", productId)
                    .parameter("unitAmount", netPurchaseAmount)
                    .call()

            // delete order item form response
            sf.sync().name("delete#mantle.order.OrderItemFormResponse")
                    .parameter("orderId", orderId)
                    .parameter("orderItemSeqId", orderItemSeqId)
                    .parameter("formResponseId", "*")
                    .call()

            // create order item form response
            sf.sync().name("create#mantle.order.OrderItemFormResponse")
                    .parameter("orderId", orderId)
                    .parameter("orderItemSeqId", orderItemSeqId)
                    .parameter("formResponseId", formResponseId)
                    .call()

            // update product category
            EntityValue productCategoryParam = ef.find("mantle.product.ProductParameterValue")
                    .condition("productParameterId", "ProductCategory")
                    .condition("productParameterSetId", productParameterSetId)
                    .list()
                    .getFirst()
            sf.sync().name("update#mantle.product.ProductParameterValue")
                    .parameter("productParameterValueId", productCategoryParam.getString("productParameterValueId"))
                    .parameter("parameterValue", productCategoryId)
                    .call()

            // update total purchase amount
            EntityValue totalPurchaseAmountParam = ef.find("mantle.product.ProductParameterValue")
                    .condition("productParameterId", "TotalPurchaseAmount")
                    .condition("productParameterSetId", productParameterSetId)
                    .list()
                    .getFirst()
            sf.sync().name("update#mantle.product.ProductParameterValue")
                    .parameter("productParameterValueId", totalPurchaseAmountParam.getString("productParameterValueId"))
                    .parameter("parameterValue", totalPurchaseAmount)
                    .call()

            // update down payment
            EntityValue downPaymentParam = ef.find("mantle.product.ProductParameterValue")
                    .condition("productParameterId", "DownPayment")
                    .condition("productParameterSetId", productParameterSetId)
                    .list()
                    .getFirst()
            sf.sync().name("update#mantle.product.ProductParameterValue")
                    .parameter("productParameterValueId", downPaymentParam.getString("productParameterValueId"))
                    .parameter("parameterValue", downPayment)
                    .call()

            // update loan fee
            EntityValue loanFeeParam = ef.find("mantle.product.ProductParameterValue")
                    .condition("productParameterId", "LoanFee")
                    .condition("productParameterSetId", productParameterSetId)
                    .list()
                    .getFirst()
            sf.sync().name("update#mantle.product.ProductParameterValue")
                    .parameter("productParameterValueId", loanFeeParam.getString("productParameterValueId"))
                    .parameter("parameterValue", loanFee)
                    .call()

            // update financed amount
            EntityValue financedAmountParam = ef.find("mantle.product.ProductParameterValue")
                    .condition("productParameterId", "FinancedAmount")
                    .condition("productParameterSetId", productParameterSetId)
                    .list()
                    .getFirst()
            sf.sync().name("update#mantle.product.ProductParameterValue")
                    .parameter("productParameterValueId", financedAmountParam.getString("productParameterValueId"))
                    .parameter("parameterValue", financedAmount)
                    .call()

            // update estimated amount
            EntityValue estimatedPaymentParam = ef.find("mantle.product.ProductParameterValue")
                    .condition("productParameterId", "EstimatedPayment")
                    .condition("productParameterSetId", productParameterSetId)
                    .list()
                    .getFirst()
            sf.sync().name("update#mantle.product.ProductParameterValue")
                    .parameter("productParameterValueId", estimatedPaymentParam.getString("productParameterValueId"))
                    .parameter("parameterValue", estimatedPayment)
                    .call()
        } else {

            // create order header
            Map<String, Object> orderHeaderResp = sf.sync().name("mantle.order.OrderServices.create#Order")
                    .parameter("salesChannelEnumId", salesChannelEnumId)
                    .parameter("enteredByPartyId", uf.getUserAccount().getString("partyId"))
                    .parameter("productStoreId", productStoreId)
                    .call()
            orderId = (String) orderHeaderResp.get("orderId")
            orderPartSeqId = (String) orderHeaderResp.get("orderPartSeqId")

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
            Map<String, Object> orderItemResp = sf.sync().name("mantle.order.OrderServices.create#OrderItem")
                    .parameter("orderId", orderId)
                    .parameter("orderPartSeqId", orderPartSeqId)
                    .parameter("productId", productId)
                    .parameter("productParameterSetId", productParameterSetId)
                    .parameter("unitAmount", netPurchaseAmount)
                    .call()
            String orderItemSeqId = (String) orderItemResp.get("orderItemSeqId")

            // create order item form response
            sf.sync().name("create#mantle.order.OrderItemFormResponse")
                    .parameter("orderId", orderId)
                    .parameter("orderItemSeqId", orderItemSeqId)
                    .parameter("formResponseId", formResponseId)
                    .call()

            // create product category
            sf.sync().name("create#mantle.product.ProductParameterValue")
                    .parameter("productParameterId", "ProductCategory")
                    .parameter("productParameterSetId", productParameterSetId)
                    .parameter("parameterValue", productCategoryId)
                    .call()

            // create total purchase amount
            sf.sync().name("create#mantle.product.ProductParameterValue")
                    .parameter("productParameterId", "TotalPurchaseAmount")
                    .parameter("productParameterSetId", productParameterSetId)
                    .parameter("parameterValue", totalPurchaseAmount)
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

            // create finance amount
            sf.sync().name("create#mantle.product.ProductParameterValue")
                    .parameter("productParameterId", "FinancedAmount")
                    .parameter("productParameterSetId", productParameterSetId)
                    .parameter("parameterValue", financedAmount)
                    .call()

            // create estimated payment
            sf.sync().name("create#mantle.product.ProductParameterValue")
                    .parameter("productParameterId", "EstimatedPayment")
                    .parameter("productParameterSetId", productParameterSetId)
                    .parameter("parameterValue", estimatedPayment)
                    .call()
        }

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("orderId", orderId)
        outParams.put("orderPartSeqId", orderPartSeqId)
        return outParams
    }

    static Map<String, Object> updateOrderItemEligibility(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        ServiceFacade sf = ec.getService()
        UserFacade uf = ec.getUser()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String orderId = (String) cs.getOrDefault("orderId", null)
        String orderPartSeqId = (String) cs.getOrDefault("orderPartSeqId", null)
        String orderItemSeqId = (String) cs.getOrDefault("orderItemSeqId", null)
        String formResponseId = (String) cs.getOrDefault("formResponseId", null)

        // find order item
        EntityValue orderItem = ef.find("mantle.order.OrderItem")
                .condition("orderId", orderId)
                .condition("orderItemSeqId", orderItemSeqId)
                .one()
        String productId = orderItem.getString("productId")

        // find product form
        EntityList formList = ef.find("mantle.product.ProductDbForm")
                .condition("productId", productId)
                .list()
        EntityValue form = formList.isEmpty() ? null : formList.getFirst()
        if (form == null) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ELIGIBILITY_FORM"))
            return new HashMap<String, Object>()
        }

        // validate form fields
        String formId = form.getString("formId")
        EntityList fieldList = ef.find("moqui.screen.form.DbFormField")
                .condition("formId", formId)
                .list()
        for (EntityValue field : fieldList) {
            long answerCount = ef.find("moqui.screen.form.FormResponseAnswer")
                    .condition("formResponseId", formResponseId)
                    .condition("formId", formId)
                    .condition("fieldName", field.getString("fieldName"))
                    .condition("valueText", "true")
                    .count()
            if (answerCount == 0) {
                mf.addError(lf.localize("DASHBOARD_APPLICANT_NOT_ELIGIBLE"))
                return new HashMap<String, Object>()
            }
        }

        // delete order item form response
        sf.sync().name("delete#mantle.order.OrderItemFormResponse")
                .parameter("orderId", orderId)
                .parameter("orderItemSeqId", orderItemSeqId)
                .parameter("formResponseId", "*")
                .call()

        // create order item form response
        sf.sync().name("create#mantle.order.OrderItemFormResponse")
                .parameter("orderId", orderId)
                .parameter("orderItemSeqId", orderItemSeqId)
                .parameter("formResponseId", formResponseId)
                .call()

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("orderId", orderId)
        outParams.put("orderPartSeqId", orderPartSeqId)
        outParams.put("orderItemSeqId", orderPartSeqId)
        return outParams
    }

    static void validateApplicantFields(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String roleTypeId = (String) cs.getOrDefault("roleTypeId", null)
        String firstName = (String) cs.getOrDefault("firstName", null)
        String middleName = (String) cs.getOrDefault("middleName", null)
        String lastName = (String) cs.getOrDefault("lastName", null)
        String suffix = (String) cs.getOrDefault("suffix", null)
        String nickname = (String) cs.getOrDefault("nickname", null)
        String address1 = (String) cs.getOrDefault("address1", null)
        String unitNumber = (String) cs.getOrDefault("unitNumber", null)
        String postalCode = (String) cs.getOrDefault("postalCode", null)
        String city = (String) cs.getOrDefault("city", null)
        String stateProvinceGeoId = (String) cs.getOrDefault("stateProvinceGeoId", null)
        Integer addressYears = (Integer) cs.getOrDefault("addressYears", 0)
        Integer addressMonths = (Integer) cs.getOrDefault("addressMonths", 0)
        String socialSecurityNumber = (String) cs.getOrDefault("socialSecurityNumber", null)
        Date birthDate = (Date) cs.getOrDefault("birthDate", null)
        String maritalStatusEnumId = (String) cs.getOrDefault("maritalStatusEnumId", null)
        String employmentStatusEnumId = (String) cs.getOrDefault("employmentStatusEnumId", null)
        String contactNumber = (String) cs.getOrDefault("contactNumber", null)
        String contactMechPurposeId = (String) cs.getOrDefault("contactMechPurposeId", null)
        String email = (String) cs.getOrDefault("email", null)
        String emailVerify = (String) cs.getOrDefault("emailVerify", null)

        // validate first name
        if (StringUtils.isBlank(firstName)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_FIRST_NAME"))
            return
        }

        // validate last name
        if (StringUtils.isBlank(lastName)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_LAST_NAME"))
            return
        }

        // validate residential address
        if (StringUtils.isBlank(address1)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_RESIDENCE_ADDR"))
            return
        }

        // validate postal code
        if (StringUtils.isBlank(postalCode)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_POSTAL_CODE"))
            return
        }

        // validate city
        if (StringUtils.isBlank(city)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_CITY"))
            return
        }

        // validate state
        if (StringUtils.isBlank(stateProvinceGeoId)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_STATE"))
            return
        }

        // validate address duration
        if (addressYears > 100 || addressMonths > 11) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ADDRESS_DURATION"))
            return
        }

        // validate social security number
        if (StringUtils.isBlank(socialSecurityNumber)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_SSN"))
            return
        }

        // validate date of birth
        Date minBirthDate = DateUtils.addYears(new Date(), -18)
        if (birthDate == null) {
            mf.addError(lf.localize("DASHBOARD_INVALID_DOB"))
            return
        } else if (birthDate.after(minBirthDate)) {
            mf.addError(lf.localize("DASHBOARD_APPLICANT_NOT_ELIGIBLE"))
            return
        }

        // validate marital status
        if (StringUtils.isBlank(maritalStatusEnumId)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_MARITAL_STATUS"))
            return
        }

        // validate employment status
        if (StringUtils.isBlank(employmentStatusEnumId)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_EMPLOYMENT_STATUS"))
            return
        }

        // validate contact number
        if (StringUtils.isBlank(contactNumber)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_PHONE_NUMBER"))
            return
        }

        // validate contact purpose
        if (StringUtils.isBlank(contactMechPurposeId)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_PHONE_NUMBER_TYPE"))
            return
        }

        // validate email address
        if (StringUtils.isNotBlank(email) || StringUtils.isNotBlank(emailVerify)) {
            if (!StringUtils.equals(email, emailVerify)) {
                mf.addError(lf.localize("DASHBOARD_INVALID_EMAIL_VERIFY"))
            }
        }
    }

    static Map<String, Object> storeApplicant(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        ServiceFacade sf = ec.getService()
        UserFacade uf = ec.getUser()
        MessageFacade mf = ec.getMessage()

        // get the parameters
        String orderId = (String) cs.getOrDefault("orderId", null)
        String orderPartSeqId = (String) cs.getOrDefault("orderPartSeqId", null)
        String partyId = (String) cs.getOrDefault("partyId", null)
        String roleTypeId = (String) cs.getOrDefault("roleTypeId", null)
        String firstName = (String) cs.getOrDefault("firstName", null)
        String middleName = (String) cs.getOrDefault("middleName", null)
        String lastName = (String) cs.getOrDefault("lastName", null)
        String suffix = (String) cs.getOrDefault("suffix", null)
        String nickname = (String) cs.getOrDefault("nickname", null)
        String address1 = (String) cs.getOrDefault("address1", null)
        String unitNumber = (String) cs.getOrDefault("unitNumber", null)
        String postalCode = (String) cs.getOrDefault("postalCode", null)
        String city = (String) cs.getOrDefault("city", null)
        String stateProvinceGeoId = (String) cs.getOrDefault("stateProvinceGeoId", null)
        Integer addressYears = (Integer) cs.getOrDefault("addressYears", 0)
        Integer addressMonths = (Integer) cs.getOrDefault("addressMonths", 0)
        String socialSecurityNumber = (String) cs.getOrDefault("socialSecurityNumber", null)
        Date birthDate = (Date) cs.getOrDefault("birthDate", null)
        String maritalStatusEnumId = (String) cs.getOrDefault("maritalStatusEnumId", null)
        String employmentStatusEnumId = (String) cs.getOrDefault("employmentStatusEnumId", null)
        String contactNumber = (String) cs.getOrDefault("contactNumber", null)
        String contactMechPurposeId = (String) cs.getOrDefault("contactMechPurposeId", null)
        String email = (String) cs.getOrDefault("email", null)

        // validate fields
        sf.sync().name("mkdecision.dashboard.OrderServices.validate#ApplicantFields")
                .parameters(cs)
                .call()
        if (mf.hasError()) {
            return new HashMap<String, Object>()
        }

        // calculate date for address duration
        Date usedSince = new Date()
        usedSince = DateUtils.addYears(usedSince, -addressYears)
        usedSince = DateUtils.addMonths(usedSince, -addressMonths)

        // store party
        boolean updateParty = StringUtils.isNotBlank(partyId)
        if (updateParty) {

            // update person
            sf.sync().name("update#mantle.party.Person")
                    .parameter("partyId", partyId)
                    .parameter("firstName", firstName)
                    .parameter("middleName", middleName)
                    .parameter("lastName", lastName)
                    .parameter("suffix", suffix)
                    .parameter("nickname", nickname)
                    .parameter("birthDate", birthDate)
                    .parameter("maritalStatusEnumId", maritalStatusEnumId)
                    .parameter("employmentStatusEnumId", employmentStatusEnumId)
                    .call()

            // update party role
            sf.sync().name("delete#mantle.party.PartyRole")
                    .parameter("partyId", partyId)
                    .parameter("roleTypeId", "*")
                    .call()
            sf.sync().name("create#mantle.party.PartyRole")
                    .parameter("partyId", partyId)
                    .parameter("roleTypeId", roleTypeId)
                    .call()

            // update postal address
            EntityValue postalAddress = ef.find("mantle.party.contact.PartyContactMechPostalAddress")
                    .condition("partyId", partyId)
                    .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                    .list()
                    .getFirst()
            sf.sync().name("update#mantle.party.contact.PostalAddress")
                    .parameter("contactMechId", postalAddress.getString("contactMechId"))
                    .parameter("address1", address1)
                    .parameter("unitNumber", unitNumber)
                    .parameter("city", city)
                    .parameter("postalCode", postalCode)
                    .parameter("stateProvinceGeoId", stateProvinceGeoId)
                    .parameter("contactMechPurposeId", "PostalPrimary")
                    .call()
            sf.sync().name("update#mantle.party.contact.PartyContactMech")
                    .parameter("partyId", postalAddress.get("partyId"))
                    .parameter("contactMechId", postalAddress.get("contactMechId"))
                    .parameter("contactMechPurposeId", postalAddress.get("contactMechPurposeId"))
                    .parameter("fromDate", postalAddress.get("fromDate"))
                    .parameter("usedSince", usedSince.getTime())
                    .call()

            // update telecom number
            EntityValue telecomNumber = ef.find("mantle.party.contact.PartyContactMechTelecomNumber")
                    .condition("partyId", partyId)
                    .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                    .list()
                    .getFirst()
            sf.sync().name("update#mantle.party.contact.TelecomNumber")
                    .parameter("contactMechId", telecomNumber.getString("contactMechId"))
                    .parameter("contactNumber", contactNumber)
                    .parameter("contactMechPurposeId", contactMechPurposeId)
                    .call()
            sf.sync().name("delete#mantle.party.contact.PartyContactMech")
                    .parameter("partyId", partyId)
                    .parameter("contactMechId", telecomNumber.getString("contactMechId"))
                    .parameter("contactMechPurposeId", telecomNumber.getString("contactMechPurposeId"))
                    .parameter("fromDate", telecomNumber.getString("fromDate"))
                    .call()
            sf.sync().name("create#mantle.party.contact.PartyContactMech")
                    .parameter("partyId", partyId)
                    .parameter("contactMechId", telecomNumber.getString("contactMechId"))
                    .parameter("contactMechPurposeId", contactMechPurposeId)
                    .parameter("fromDate", uf.getNowTimestamp())
                    .call()

            // update email address
            EntityValue info = ef.find("mantle.party.contact.PartyContactMechInfo")
                    .condition("partyId", partyId)
                    .condition("contactMechPurposeId", "EmailPrimary")
                    .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                    .list()
                    .getFirst()
            if (info == null) {
                sf.sync().name("mantle.party.ContactServices.create#EmailAddress")
                        .parameter("partyId", partyId)
                        .parameter("emailAddress", email)
                        .parameter("contactMechPurposeId", "EmailPrimary")
                        .call()
            } else {
                sf.sync().name("update#mantle.party.contact.ContactMech")
                        .parameter("contactMechId", info.getString("contactMechId"))
                        .parameter("infoString", email)
                        .call()
            }
        } else {

            // create person
            Map<String, Object> personResp = sf.sync().name("mantle.party.PartyServices.create#Person")
                    .parameter("partyTypeEnumId", "PtyPerson")
                    .parameter("firstName", firstName)
                    .parameter("middleName", middleName)
                    .parameter("lastName", lastName)
                    .parameter("suffix", suffix)
                    .parameter("nickname", nickname)
                    .parameter("birthDate", birthDate)
                    .parameter("maritalStatusEnumId", maritalStatusEnumId)
                    .parameter("employmentStatusEnumId", employmentStatusEnumId)
                    .parameter("roleTypeId", roleTypeId)
                    .call()
            partyId = (String) personResp.get("partyId")

            // create order part party
            sf.sync().name("create#mantle.order.OrderPartParty")
                    .parameter("orderId", orderId)
                    .parameter("orderPartSeqId", orderPartSeqId)
                    .parameter("partyId", partyId)
                    .parameter("roleTypeId", roleTypeId)
                    .call()

            // update order part customer
            if (StringUtils.equals(roleTypeId, "PrimaryApplicant")) {
                sf.sync().name("update#mantle.order.OrderPart")
                        .parameter("orderId", orderId)
                        .parameter("orderPartSeqId", orderPartSeqId)
                        .parameter("customerPartyId", partyId)
                        .call()
            }

            // create postal address
            sf.sync().name("mantle.party.ContactServices.create#PostalAddress")
                    .parameter("partyId", partyId)
                    .parameter("address1", address1)
                    .parameter("unitNumber", unitNumber)
                    .parameter("city", city)
                    .parameter("postalCode", postalCode)
                    .parameter("stateProvinceGeoId", stateProvinceGeoId)
                    .parameter("usedSince", usedSince.getTime())
                    .parameter("contactMechPurposeId", "PostalPrimary")
                    .call()

            // create telecom number
            sf.sync().name("mantle.party.ContactServices.create#TelecomNumber")
                    .parameter("partyId", partyId)
                    .parameter("contactNumber", contactNumber)
                    .parameter("contactMechPurposeId", contactMechPurposeId)
                    .call()

            // create email address
            if (StringUtils.isNotBlank(email)) {
                sf.sync().name("mantle.party.ContactServices.create#EmailAddress")
                        .parameter("partyId", partyId)
                        .parameter("emailAddress", email)
                        .parameter("contactMechPurposeId", "EmailPrimary")
                        .call()
            }
        }

        // create social security number
        sf.sync().name("delete#mantle.party.PartyIdentification")
                .parameter("partyId", partyId)
                .parameter("partyIdTypeEnumId", "PtidSsn")
                .call()
        sf.sync().name("create#mantle.party.PartyIdentification")
                .parameter("partyId", partyId)
                .parameter("partyIdTypeEnumId", "PtidSsn")
                .parameter("idValue", socialSecurityNumber)
                .call()

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("partyId", partyId)
        return outParams
    }

    static Map<String, Object> archiveOrderParty(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        ServiceFacade sf = ec.getService()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String orderId = (String) cs.getOrDefault("orderId", null)
        String orderPartSeqId = (String) cs.getOrDefault("orderPartSeqId", null)
        String partyId = (String) cs.getOrDefault("partyId", null)

        // find party
        EntityValue party = ef.find("mantle.order.OrderPartParty")
                .condition("orderId", orderId)
                .condition("orderPartSeqId", orderPartSeqId)
                .condition("partyId", partyId)
                .one()

        // validate party
        if (party == null) {
            mf.addError(lf.localize("DASHBOARD_INVALID_PARTY"))
            return new HashMap<String, Object>()
        }

        // delete party
        sf.sync().name("delete#mantle.order.OrderPartParty")
                .parameter("orderId", orderId)
                .parameter("orderPartSeqId", orderPartSeqId)
                .parameter("partyId", partyId)
                .parameter("roleTypeId", party.getString("roleTypeId"))
                .call()

        // create party with archived status
        sf.sync().name("create#mantle.order.OrderPartParty")
                .parameter("orderId", orderId)
                .parameter("orderPartSeqId", orderPartSeqId)
                .parameter("partyId", partyId)
                .parameter("roleTypeId", "Archived")
                .call()

        // return the output parameters
        return new HashMap<>()
    }

    static void validatePropertyFields(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String classEnumId = (String) cs.getOrDefault("classEnumId", null)
        BigDecimal salvageValue = (BigDecimal) cs.getOrDefault("salvageValue", null)
        BigDecimal acquireCost = (BigDecimal) cs.getOrDefault("acquireCost", null)
        BigDecimal hoaFeeMonthly = (BigDecimal) cs.getOrDefault("hoaFeeMonthly", null)
        BigDecimal propertyTaxesMonthly = (BigDecimal) cs.getOrDefault("propertyTaxesMonthly", null)
        BigDecimal propertyInsuranceCostsMonthly = (BigDecimal) cs.getOrDefault("propertyInsuranceCostsMonthly", null)

        // validate asset class
        if (StringUtils.isBlank(classEnumId)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ASSET_CLASS"))
            return
        }

        // validate salvage value
        if (salvageValue == null || salvageValue <= 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_SALVAGE_VALUE"))
            return
        }

        // validate acquire cost
        if (acquireCost == null || acquireCost < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ACQUIRE_COST"))
            return
        }

        // validate HOA monthly fee
        if (hoaFeeMonthly == null || hoaFeeMonthly < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_HOA_FEE_MONTHLY"))
            return
        }

        // validate property tax monthly
        if (propertyTaxesMonthly == null || propertyTaxesMonthly < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_PROPERTY_TAX_MONTHLY"))
            return
        }

        // validate property insurance cost monthly
        if (propertyInsuranceCostsMonthly == null || propertyInsuranceCostsMonthly < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_PROPERTY_INSURANCE_COST_MONTHLY"))
            return
        }
    }

    static Map<String, Object> storeProperty(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        ServiceFacade sf = ec.getService()
        UserFacade uf = ec.getUser()
        MessageFacade mf = ec.getMessage()

        // get the parameters
        String orderId = (String) cs.getOrDefault("orderId", null)
        String orderPartSeqId = (String) cs.getOrDefault("orderPartSeqId", null)
        String partyId = (String) cs.getOrDefault("partyId", null)
        String assetId = (String) cs.getOrDefault("assetId", null)
        String classEnumId = (String) cs.getOrDefault("classEnumId", null)
        BigDecimal salvageValue = (BigDecimal) cs.getOrDefault("salvageValue", null)
        BigDecimal acquireCost = (BigDecimal) cs.getOrDefault("acquireCost", null)
        BigDecimal hoaFeeMonthly = (BigDecimal) cs.getOrDefault("hoaFeeMonthly", null)
        BigDecimal propertyTaxesMonthly = (BigDecimal) cs.getOrDefault("propertyTaxesMonthly", null)
        BigDecimal propertyInsuranceCostsMonthly = (BigDecimal) cs.getOrDefault("propertyInsuranceCostsMonthly", null)

        // validate fields
        sf.sync().name("mkdecision.dashboard.OrderServices.validate#PropertyFields")
                .parameters(cs)
                .call()
        if (mf.hasError()) {
            return new HashMap<String, Object>()
        }

        // store asset
        boolean updateAsset = StringUtils.isNotBlank(assetId)
        if (updateAsset) {

            // update asset
            sf.sync().name("update#mantle.product.asset.Asset")
                    .parameter("assetId", assetId)
                    .parameter("classEnumId", classEnumId)
                    .parameter("salvageValue", salvageValue)
                    .parameter("acquireCost", acquireCost)
                    .call()

            // delete asset expenses
            EntityList financialFlowList = ef.find("mk.close.FinancialFlow")
                    .condition("partyId", partyId)
                    .condition("entryTypeEnumId", "MkEntryExpense")
                    .condition("assetId", assetId)
                    .list()
            for (EntityValue financialFlow : financialFlowList) {
                sf.sync().name("delete#mk.close.FinancialFlow")
                        .parameter("financialFlowId", financialFlow.getString("financialFlowId"))
                        .call()
            }
        } else {

            // create facility
            Map<String, Object> facilityResp = sf.sync().name("create#mantle.facility.Facility")
                    .parameter("facilityTypeEnumId", "FcTpHeadquarters")
                    .call()
            String facilityId = (String) facilityResp.get("facilityId")

            // create facility contact mech
            EntityValue postalAddress = ef.find("mantle.party.contact.PartyContactMechPostalAddress")
                    .condition("partyId", partyId)
                    .condition("contactMechTypeEnumId", "CmtPostalAddress")
                    .condition("contactMechPurposeId", "PostalPrimary")
                    .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                    .list()
                    .getFirst()
            String contactMechId = postalAddress.getString("contactMechId")
            sf.sync().name("create#mantle.facility.FacilityContactMech")
                    .parameter("facilityId", facilityId)
                    .parameter("contactMechId", contactMechId)
                    .parameter("contactMechPurposeId", "PostalProperty")
                    .parameter("fromDate", uf.getNowTimestamp())
                    .call()

            // create asset
            Map<String, Object> assetResp = sf.sync().name("create#mantle.product.asset.Asset")
                    .parameter("assetId", assetId)
                    .parameter("assetTypeEnumId", "AstTpRealEstate")
                    .parameter("classEnumId", classEnumId)
                    .parameter("facilityId", facilityId)
                    .parameter("salvageValue", salvageValue)
                    .parameter("acquireCost", acquireCost)
                    .parameter("ownerPartyId", partyId)
                    .call()
            assetId = (String) assetResp.get("assetId")
        }

        // create HOA monthly fee
        sf.sync().name("create#mk.close.FinancialFlow")
                .parameter("partyId", partyId)
                .parameter("entryTypeEnumId", "MkEntryExpense")
                .parameter("financialFlowTypeEnumId", "MkFinFlowHoaMonthlyFee")
                .parameter("assetId", assetId)
                .parameter("amount", hoaFeeMonthly)
                .call()

        // create monthly property taxes
        sf.sync().name("create#mk.close.FinancialFlow")
                .parameter("partyId", partyId)
                .parameter("entryTypeEnumId", "MkEntryExpense")
                .parameter("financialFlowTypeEnumId", "MkFinFlowMonthlyInsuranceCosts")
                .parameter("assetId", assetId)
                .parameter("amount", propertyTaxesMonthly)
                .call()

        // create monthly insurance costs
        sf.sync().name("create#mk.close.FinancialFlow")
                .parameter("partyId", partyId)
                .parameter("entryTypeEnumId", "MkEntryExpense")
                .parameter("financialFlowTypeEnumId", "MkFinFlowMonthlyPropertyTaxes")
                .parameter("assetId", assetId)
                .parameter("amount", propertyInsuranceCostsMonthly)
                .call()

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("assetId", assetId)
        return outParams
    }

    static void validateOrderItemFields(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        UserFacade uf = ec.getUser()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String orderId = (String) cs.getOrDefault("orderId", null)
        String productCategoryId = (String) cs.getOrDefault("productCategoryId", null)
        String productId = (String) cs.getOrDefault("productId", null)
        BigDecimal totalPurchaseAmount = (BigDecimal) cs.getOrDefault("totalPurchaseAmount", null)
        BigDecimal downPayment = (BigDecimal) cs.getOrDefault("downPayment", null)
        BigDecimal netPurchaseAmount = (BigDecimal) cs.getOrDefault("netPurchaseAmount", null)
        BigDecimal loanFee = (BigDecimal) cs.getOrDefault("loanFee", null)
        BigDecimal financedAmount = (BigDecimal) cs.getOrDefault("financedAmount", null)
        BigDecimal estimatedPayment = (BigDecimal) cs.getOrDefault("estimatedPayment", null)

        // validate order header
        EntityValue orderHeader = ef.find("mantle.order.OrderHeader")
                .condition("orderId", orderId)
                .one()
        if (orderHeader == null) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ORDER"))
            return
        }

        // validate product store
        EntityFacadeImpl efi = (EntityFacadeImpl) ef
        EntityConditionFactory ecf = efi.getConditionFactory()
        String productStoreId = orderHeader.getString("productStoreId")
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
            mf.addError(lf.localize("DASHBOARD_INVALID_PRODUCT_STORE"))
            return
        }

        // validate product category
        long productCategoryCount = ef.find("mkdecision.dashboard.ProductStoreCategoryDetail")
                .condition("productStoreId", productStoreId)
                .condition("storeCategoryTypeEnumId", "PsctFinanceableProducts")
                .condition("productCategoryId", productCategoryId)
                .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                .count()
        if (productCategoryCount == 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_PRODUCT_CATEGORY"))
            return
        }

        // validate product category member
        long categoryMemberCount = ef.find("mantle.product.category.ProductCategoryMember")
                .condition("productCategoryId", productCategoryId)
                .condition("productId", productId)
                .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                .count()
        if (categoryMemberCount == 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_PRODUCT"))
            return
        }

        // validate total purchase amount
        if (totalPurchaseAmount == null || totalPurchaseAmount <= 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_TOTAL_PURCHASE_AMOUNT"))
            return
        }

        // validate down payment
        if (downPayment == null || downPayment < 0 || downPayment > totalPurchaseAmount) {
            mf.addError(lf.localize("DASHBOARD_INVALID_DOWN_PAYMENT"))
            return
        }

        // validate net purchase amount
        if (netPurchaseAmount == null || netPurchaseAmount != (totalPurchaseAmount - downPayment)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_NET_PURCHASE_AMOUNT"))
            return
        }

        // validate loan fee
        if (loanFee == null || loanFee < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_LOAN_FEE"))
            return
        }

        // validate financed amount
        BigDecimal financedAmountBigDecimal = new BigDecimal(financedAmount)
        if (financedAmountBigDecimal == null || financedAmountBigDecimal != ((totalPurchaseAmount + loanFee) - downPayment)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_FINANCED_AMOUNT"))
            return
        }

        // validate estimated amount
        if (estimatedPayment == null || estimatedPayment <= 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ESTIMATED_AMOUNT"))
        }
    }

    static Map<String, Object> addOrderItem(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        ServiceFacade sf = ec.getService()
        MessageFacade mf = ec.getMessage()

        // get the parameters
        String orderId = (String) cs.getOrDefault("orderId", null)
        String orderPartSeqId = (String) cs.getOrDefault("orderPartSeqId", null)
        String productCategoryId = (String) cs.getOrDefault("productCategoryId", null)
        String productId = (String) cs.getOrDefault("productId", null)
        BigDecimal totalPurchaseAmount = (BigDecimal) cs.getOrDefault("totalPurchaseAmount", null)
        BigDecimal downPayment = (BigDecimal) cs.getOrDefault("downPayment", null)
        BigDecimal netPurchaseAmount = (BigDecimal) cs.getOrDefault("netPurchaseAmount", null)
        BigDecimal loanFee = (BigDecimal) cs.getOrDefault("loanFee", null)
        BigDecimal financedAmount = (BigDecimal) cs.getOrDefault("financedAmount", null)
        BigDecimal estimatedPayment = (BigDecimal) cs.getOrDefault("estimatedPayment", null)

        // validate fields
        sf.sync().name("mkdecision.dashboard.OrderServices.validate#OrderItemFields")
                .parameters(cs)
                .call()
        if (mf.hasError()) {
            return new HashMap<String, Object>()
        }

        // create product parameter set
        Map<String, Object> productParameterSetResp = sf.sync().name("create#mantle.product.ProductParameterSet")
                .parameter("productId", productId)
                .call()
        String productParameterSetId = (String) productParameterSetResp.get("productParameterSetId")

        // create order item
        Map<String, Object> orderItemResp = sf.sync().name("mantle.order.OrderServices.create#OrderItem")
                .parameter("orderId", orderId)
                .parameter("orderPartSeqId", orderPartSeqId)
                .parameter("productId", productId)
                .parameter("productParameterSetId", productParameterSetId)
                .parameter("unitAmount", netPurchaseAmount)
                .call()
        String orderItemSeqId = (String) orderItemResp.get("orderItemSeqId")

        // create product category
        sf.sync().name("create#mantle.product.ProductParameterValue")
                .parameter("productParameterId", "ProductCategory")
                .parameter("productParameterSetId", productParameterSetId)
                .parameter("parameterValue", productCategoryId)
                .call()

        // create total purchase amount
        sf.sync().name("create#mantle.product.ProductParameterValue")
                .parameter("productParameterId", "TotalPurchaseAmount")
                .parameter("productParameterSetId", productParameterSetId)
                .parameter("parameterValue", totalPurchaseAmount)
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

        // create financed amount
        sf.sync().name("create#mantle.product.ProductParameterValue")
                .parameter("productParameterId", "FinancedAmount")
                .parameter("productParameterSetId", productParameterSetId)
                .parameter("parameterValue", financedAmount)
                .call()

        // create estimated payment
        sf.sync().name("create#mantle.product.ProductParameterValue")
                .parameter("productParameterId", "EstimatedPayment")
                .parameter("productParameterSetId", productParameterSetId)
                .parameter("parameterValue", estimatedPayment)
                .call()

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("orderId", orderId)
        outParams.put("orderItemSeqId", orderItemSeqId)
        return outParams
    }

    static Map<String, Object> updateOrderItem(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        ServiceFacade sf = ec.getService()
        MessageFacade mf = ec.getMessage()

        // get the parameters
        String orderId = (String) cs.getOrDefault("orderId", null)
        String orderPartSeqId = (String) cs.getOrDefault("orderPartSeqId", null)
        String productCategoryId = (String) cs.getOrDefault("productCategoryId", null)
        String productId = (String) cs.getOrDefault("productId", null)
        BigDecimal totalPurchaseAmount = (BigDecimal) cs.getOrDefault("totalPurchaseAmount", null)
        BigDecimal downPayment = (BigDecimal) cs.getOrDefault("downPayment", null)
        BigDecimal netPurchaseAmount = (BigDecimal) cs.getOrDefault("netPurchaseAmount", null)
        BigDecimal loanFee = (BigDecimal) cs.getOrDefault("loanFee", null)
        BigDecimal financedAmount = (BigDecimal) cs.getOrDefault("financedAmount", null)
        BigDecimal estimatedPayment = (BigDecimal) cs.getOrDefault("estimatedPayment", null)

        // validate fields
        sf.sync().name("mkdecision.dashboard.OrderServices.validate#OrderItemFields")
                .parameters(cs)
                .call()
        if (mf.hasError()) {
            return new HashMap<String, Object>()
        }

        // find order item
        EntityValue orderItem = ef.find("mantle.order.OrderItem")
                .condition("orderId", orderId)
                .condition("orderPartSeqId", orderPartSeqId)
                .orderBy("-lastUpdatedStamp")
                .list()
                .getFirst()
        String orderItemSeqId = orderItem.getString("orderItemSeqId")
        String productParameterSetId = orderItem.getString("productParameterSetId")

        // update order item
        sf.sync().name("mantle.order.OrderServices.update#OrderItem")
                .parameter("orderId", orderId)
                .parameter("orderPartSeqId", orderPartSeqId)
                .parameter("orderItemSeqId", orderItemSeqId)
                .parameter("productId", productId)
                .parameter("unitAmount", netPurchaseAmount)
                .call()

        // update product category
        EntityValue productCategoryParam = ef.find("mantle.product.ProductParameterValue")
                .condition("productParameterId", "ProductCategory")
                .condition("productParameterSetId", productParameterSetId)
                .list()
                .getFirst()
        sf.sync().name("update#mantle.product.ProductParameterValue")
                .parameter("productParameterValueId", productCategoryParam.getString("productParameterValueId"))
                .parameter("parameterValue", productCategoryId)
                .call()

        // update total purchase amount
        EntityValue totalPurchaseAmountParam = ef.find("mantle.product.ProductParameterValue")
                .condition("productParameterId", "TotalPurchaseAmount")
                .condition("productParameterSetId", productParameterSetId)
                .list()
                .getFirst()
        sf.sync().name("update#mantle.product.ProductParameterValue")
                .parameter("productParameterValueId", totalPurchaseAmountParam.getString("productParameterValueId"))
                .parameter("parameterValue", totalPurchaseAmount)
                .call()

        // update down payment
        EntityValue downPaymentParam = ef.find("mantle.product.ProductParameterValue")
                .condition("productParameterId", "DownPayment")
                .condition("productParameterSetId", productParameterSetId)
                .list()
                .getFirst()
        sf.sync().name("update#mantle.product.ProductParameterValue")
                .parameter("productParameterValueId", downPaymentParam.getString("productParameterValueId"))
                .parameter("parameterValue", downPayment)
                .call()

        // update loan fee
        EntityValue loanFeeParam = ef.find("mantle.product.ProductParameterValue")
                .condition("productParameterId", "LoanFee")
                .condition("productParameterSetId", productParameterSetId)
                .list()
                .getFirst()
        sf.sync().name("update#mantle.product.ProductParameterValue")
                .parameter("productParameterValueId", loanFeeParam.getString("productParameterValueId"))
                .parameter("parameterValue", loanFee)
                .call()

        // update financed amount
        EntityValue financedAmountParam = ef.find("mantle.product.ProductParameterValue")
                .condition("productParameterId", "FinancedAmount")
                .condition("productParameterSetId", productParameterSetId)
                .list()
                .getFirst()
        sf.sync().name("update#mantle.product.ProductParameterValue")
                .parameter("productParameterValueId", financedAmountParam.getString("productParameterValueId"))
                .parameter("parameterValue", financedAmount)
                .call()

        // update estimated amount
        EntityValue estimatedPaymentParam = ef.find("mantle.product.ProductParameterValue")
                .condition("productParameterId", "EstimatedPayment")
                .condition("productParameterSetId", productParameterSetId)
                .list()
                .getFirst()
        sf.sync().name("update#mantle.product.ProductParameterValue")
                .parameter("productParameterValueId", estimatedPaymentParam.getString("productParameterValueId"))
                .parameter("parameterValue", estimatedPayment)
                .call()

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("orderId", orderId)
        outParams.put("orderItemSeqId", orderItemSeqId)
        return outParams
    }

    static Map<String, Object> deleteOrderItem(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        ServiceFacade sf = ec.getService()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String orderId = (String) cs.getOrDefault("orderId", null)
        String orderPartSeqId = (String) cs.getOrDefault("orderPartSeqId", null)
        String orderItemSeqId = (String) cs.getOrDefault("orderItemSeqId", null)

        // find order item
        EntityValue item = ef.find("mantle.order.OrderItem")
                .condition("orderId", orderId)
                .condition("orderItemSeqId", orderItemSeqId)
                .one()

        // validate item
        if (item == null) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ORDER_ITEM"))
            return new HashMap<String, Object>()
        }

        // TODO: Cleanup product parameter set?

        // delete order item
        sf.sync().name("mantle.order.OrderServices.delete#OrderItem")
                .parameter("orderId", orderId)
                .parameter("orderItemSeqId", orderItemSeqId)
                .call()

        // return the output parameters
        return new HashMap<>()
    }
}
