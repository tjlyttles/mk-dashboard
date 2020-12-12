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

    static Map<String, Object> validateOrderAccess(ExecutionContext ec) {

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
            return new HashMap<String, Object>()
        }

        // return the output parameters
        return new HashMap<>()
    }

    static Map<String, Object> validateOrderFields(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
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
            return new HashMap<String, Object>()
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
                return new HashMap<String, Object>()
            }
        } else if (salesRepresentativeId != userPartyId) {
            mf.addError(lf.localize("DASHBOARD_INVALID_SALES_REP"))
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
            mf.addError(lf.localize("DASHBOARD_INVALID_PRODUCT_CATEGORY"))
            return new HashMap<String, Object>()
        }

        // validate product category member
        long categoryMemberCount = ef.find("mantle.product.category.ProductCategoryMember")
                .condition("productCategoryId", productCategoryId)
                .condition("productId", productId)
                .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                .count()
        if (categoryMemberCount == 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_PRODUCT"))
            return new HashMap<String, Object>()
        }

        // validate total purchase amount
        if (totalPurchaseAmount == null || totalPurchaseAmount <= 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_TOTAL_PURCHASE_AMOUNT"))
            return new HashMap<String, Object>()
        }

        // validate down payment
        if (downPayment == null || downPayment < 0 || downPayment > totalPurchaseAmount) {
            mf.addError(lf.localize("DASHBOARD_INVALID_DOWN_PAYMENT"))
            return new HashMap<String, Object>()
        }

        // validate net purchase amount
        if (netPurchaseAmount == null || netPurchaseAmount != (totalPurchaseAmount - downPayment)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_NET_PURCHASE_AMOUNT"))
            return new HashMap<String, Object>()
        }

        // validate loan fee
        if (loanFee == null || loanFee < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_LOAN_FEE"))
            return new HashMap<String, Object>()
        }

        // validate financed amount
        BigDecimal financedAmountBigDecimal = new BigDecimal(financedAmount)
        if (financedAmountBigDecimal == null || financedAmountBigDecimal != ((totalPurchaseAmount + loanFee) - downPayment)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_FINANCED_AMOUNT"))
            return new HashMap<String, Object>()
        }

        // validate estimated amount
        if (estimatedPayment == null || estimatedPayment <= 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ESTIMATED_AMOUNT"))
            return new HashMap<String, Object>()
        }

        // return the output parameters
        return new HashMap<>()
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

    static Map<String, Object> validateApplicantFields(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String orderId = (String) cs.getOrDefault("orderId", null)
        String orderPartSeqId = (String) cs.getOrDefault("orderPartSeqId", null)
        String partyId = (String) cs.getOrDefault("partyId", null)
        String roleTypeId = (String) cs.getOrDefault("roleTypeId", null)
        String firstName = (String) cs.getOrDefault("firstName", null)
        String middleName = (String) cs.getOrDefault("middleName", null)
        String lastName = (String) cs.getOrDefault("lastName", null)
        String suffix = (String) cs.getOrDefault("suffix", null)
        String address1 = (String) cs.getOrDefault("address1", null)
        String unitNumber = (String) cs.getOrDefault("unitNumber", null)
        String postalCode = (String) cs.getOrDefault("postalCode", null)
        String city = (String) cs.getOrDefault("city", null)
        String stateProvinceGeoId = (String) cs.getOrDefault("stateProvinceGeoId", null)
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
            return new HashMap<String, Object>()
        }

        // validate last name
        if (StringUtils.isBlank(lastName)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_LAST_NAME"))
            return new HashMap<String, Object>()
        }

        // validate residential address
        if (StringUtils.isBlank(address1)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_RESIDENCE_ADDR"))
            return new HashMap<String, Object>()
        }

        // validate postal code
        if (StringUtils.isBlank(postalCode)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_POSTAL_CODE"))
            return new HashMap<String, Object>()
        }

        // validate city
        if (StringUtils.isBlank(city)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_CITY"))
            return new HashMap<String, Object>()
        }

        // validate state
        if (StringUtils.isBlank(stateProvinceGeoId)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_STATE"))
            return new HashMap<String, Object>()
        }

        // validate social security number
        if (StringUtils.isBlank(socialSecurityNumber)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_SSN"))
            return new HashMap<String, Object>()
        }

        // validate date of birth
        Date minBirthDate = DateUtils.addYears(new Date(), -18)
        if (birthDate == null) {
            mf.addError(lf.localize("DASHBOARD_INVALID_DOB"))
            return new HashMap<String, Object>()
        } else if (birthDate.after(minBirthDate)) {
            mf.addError(lf.localize("DASHBOARD_APPLICANT_NOT_ELIGIBLE"))
            return new HashMap<String, Object>()
        }

        // validate marital status
        if (StringUtils.isBlank(maritalStatusEnumId)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_MARITAL_STATUS"))
            return new HashMap<String, Object>()
        }

        // validate employment status
        if (StringUtils.isBlank(employmentStatusEnumId)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_EMPLOYMENT_STATUS"))
            return new HashMap<String, Object>()
        }

        // validate contact number
        if (StringUtils.isBlank(contactNumber)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_PHONE_NUMBER"))
            return new HashMap<String, Object>()
        }

        // validate contact purpose
        if (StringUtils.isBlank(contactMechPurposeId)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_PHONE_NUMBER_TYPE"))
            return new HashMap<String, Object>()
        }

        // validate email address
        if (StringUtils.isBlank(email)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_EMAIL"))
            return new HashMap<String, Object>()
        } else if (!StringUtils.equals(email, emailVerify)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_EMAIL_VERIFY"))
            return new HashMap<String, Object>()
        }

        // return the output parameters
        return new HashMap<>()
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
        String address1 = (String) cs.getOrDefault("address1", null)
        String unitNumber = (String) cs.getOrDefault("unitNumber", null)
        String postalCode = (String) cs.getOrDefault("postalCode", null)
        String city = (String) cs.getOrDefault("city", null)
        String stateProvinceGeoId = (String) cs.getOrDefault("stateProvinceGeoId", null)
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
            sf.sync().name("update#mantle.party.contact.ContactMech")
                    .parameter("contactMechId", info.getString("contactMechId"))
                    .parameter("infoString", email)
                    .call()
        } else {

            // create person
            Map<String, Object> personResp = sf.sync().name("mantle.party.PartyServices.create#Person")
                    .parameter("partyTypeEnumId", "PtyPerson")
                    .parameter("firstName", firstName)
                    .parameter("middleName", middleName)
                    .parameter("lastName", lastName)
                    .parameter("suffix", suffix)
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
                    .parameter("contactMechPurposeId", "PostalPrimary")
                    .call()

            // create telecom number
            sf.sync().name("mantle.party.ContactServices.create#TelecomNumber")
                    .parameter("partyId", partyId)
                    .parameter("contactNumber", contactNumber)
                    .parameter("contactMechPurposeId", contactMechPurposeId)
                    .call()

            // create email address
            sf.sync().name("mantle.party.ContactServices.create#EmailAddress")
                    .parameter("partyId", partyId)
                    .parameter("emailAddress", email)
                    .parameter("contactMechPurposeId", "EmailPrimary")
                    .call()
        }

        // delete identifications
        sf.sync().name("delete#mantle.party.PartyIdentification")
                .parameter("partyId", partyId)
                .parameter("partyIdTypeEnumId", "PtidSsn")
                .call()

        // create social security number
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

    static Map<String, Object> validateIdentityFields(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String orderId = (String) cs.getOrDefault("orderId", null)
        String orderPartSeqId = (String) cs.getOrDefault("orderPartSeqId", null)
        String partyId = (String) cs.getOrDefault("partyId", null)
        String partyIdTypeEnumId = (String) cs.getOrDefault("partyIdTypeEnumId", null)
        String idIssuedBy = (String) cs.getOrDefault("idIssuedBy", null)
        String idValue = (String) cs.getOrDefault("idValue", null)
        Date idIssueDate = (Date) cs.getOrDefault("idIssueDate", null)
        Date idExpiryDate = (Date) cs.getOrDefault("idExpiryDate", null)

        // validate ID type
        if (StringUtils.isBlank(partyIdTypeEnumId)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ID_TYPE"))
            return new HashMap<String, Object>()
        }

        // validate ID value
        if (StringUtils.isBlank(idValue)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ID_VALUE"))
            return new HashMap<String, Object>()
        }

        // validate ID issued by
        if (StringUtils.isBlank(idIssuedBy)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ID_ISSUER"))
            return new HashMap<String, Object>()
        }

        // validate issue date
        if (idIssueDate == null && !StringUtils.equals(partyIdTypeEnumId, "PtidArn")) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ID_ISSUE_DATE"))
            return new HashMap<String, Object>()
        }

        // validate expiry date
        if (idExpiryDate == null) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ID_EXPIRY_DATE"))
            return new HashMap<String, Object>()
        }

        // return the output parameters
        return new HashMap<>()
    }

    static Map<String, Object> addIdentity(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        ServiceFacade sf = ec.getService()
        MessageFacade mf = ec.getMessage()

        // get the parameters
        String orderId = (String) cs.getOrDefault("orderId", null)
        String orderPartSeqId = (String) cs.getOrDefault("orderPartSeqId", null)
        String partyId = (String) cs.getOrDefault("partyId", null)
        String partyIdTypeEnumId = (String) cs.getOrDefault("partyIdTypeEnumId", null)
        String idIssuedBy = (String) cs.getOrDefault("idIssuedBy", null)
        String idValue = (String) cs.getOrDefault("idValue", null)
        Date idIssueDate = (Date) cs.getOrDefault("idIssueDate", null)
        Date idExpiryDate = (Date) cs.getOrDefault("idExpiryDate", null)

        // validate fields
        sf.sync().name("mkdecision.dashboard.OrderServices.validate#IdentityFields")
                .parameters(cs)
                .call()
        if (mf.hasError()) {
            return new HashMap<String, Object>()
        }

        // create identity
        sf.sync().name("create#mantle.party.PartyIdentification")
                .parameter("partyId", partyId)
                .parameter("partyIdTypeEnumId", partyIdTypeEnumId)
                .parameter("idValue", idValue)
                .parameter("issuedBy", idIssuedBy)
                .parameter("issueDate", idIssueDate)
                .parameter("expireDate", idExpiryDate)
                .call()

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("partyId", partyId)
        outParams.put("partyIdTypeEnumId", partyIdTypeEnumId)
        return outParams
    }

    static Map<String, Object> deleteIdentity(ExecutionContext ec) {

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
        String partyIdTypeEnumId = (String) cs.getOrDefault("partyIdTypeEnumId", null)

        // find identity
        EntityValue relationship = ef.find("mantle.party.PartyIdentification")
                .condition("partyId", partyId)
                .condition("partyIdTypeEnumId", partyIdTypeEnumId)
                .one()

        // validate identity
        if (relationship == null) {
            mf.addError(lf.localize("DASHBOARD_INVALID_IDENTITY"))
            return new HashMap<String, Object>()
        }

        // delete identity
        sf.sync().name("delete#mantle.party.PartyIdentification")
                .parameter("partyId", partyId)
                .parameter("partyIdTypeEnumId", partyIdTypeEnumId)
                .call()

        // return the output parameters
        return new HashMap<>()
    }

    static Map<String, Object> validateEmploymentFields(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String orderId = (String) cs.getOrDefault("orderId", null)
        String orderPartSeqId = (String) cs.getOrDefault("orderPartSeqId", null)
        String partyId = (String) cs.getOrDefault("partyId", null)
        String occupation = (String) cs.getOrDefault("occupation", null)
        String employerName = (String) cs.getOrDefault("employerName", null)
        String jobTitle = (String) cs.getOrDefault("jobTitle", null)
        Integer years = (Integer) cs.getOrDefault("years", null)
        Integer months = (Integer) cs.getOrDefault("months", null)
        BigDecimal monthlyIncome = (BigDecimal) cs.getOrDefault("monthlyIncome", null)
        String employerAddress1 = (String) cs.getOrDefault("employerAddress1", null)
        String employerUnitNumber = (String) cs.getOrDefault("employerUnitNumber", null)
        String employerPostalCode = (String) cs.getOrDefault("employerPostalCode", null)
        String employerCity = (String) cs.getOrDefault("employerCity", null)
        String employerStateProvinceGeoId = (String) cs.getOrDefault("employerStateProvinceGeoId", null)
        String employerContactNumber = (String) cs.getOrDefault("employerContactNumber", null)
        BigDecimal rentIncome = (BigDecimal) cs.getOrDefault("rentIncome", null)
        BigDecimal retirementIncome = (BigDecimal) cs.getOrDefault("retirementIncome", null)
        BigDecimal investmentIncome = (BigDecimal) cs.getOrDefault("investmentIncome", null)
        BigDecimal bonusIncome = (BigDecimal) cs.getOrDefault("bonusIncome", null)
        BigDecimal socialSecurityIncome = (BigDecimal) cs.getOrDefault("socialSecurityIncome", null)
        BigDecimal disabilityIncome = (BigDecimal) cs.getOrDefault("disabilityIncome", null)
        BigDecimal studentIncome = (BigDecimal) cs.getOrDefault("studentIncome", null)
        BigDecimal unemploymentIncome = (BigDecimal) cs.getOrDefault("unemploymentIncome", null)
        BigDecimal alimonyIncome = (BigDecimal) cs.getOrDefault("alimonyIncome", null)
        BigDecimal childSupportIncome = (BigDecimal) cs.getOrDefault("childSupportIncome", null)
        BigDecimal otherIncome = (BigDecimal) cs.getOrDefault("otherIncome", null)

        // validate occupation
        if (StringUtils.isBlank(occupation)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_OCCUPATION"))
            return new HashMap<String, Object>()
        }

        // validate employment fields
        long employmentCount = StringUtils.isBlank(partyId) ? 0 : ef.find("mantle.party.PartyRelationship")
                .condition("relationshipTypeEnumId", "PrtEmployee")
                .condition("fromPartyId", partyId)
                .count()
        if (employmentCount == 0) {

            // validate employer name
            if (StringUtils.isBlank(employerName)) {
                mf.addError(lf.localize("DASHBOARD_INVALID_EMPLOYER_NAME"))
                return new HashMap<String, Object>()
            }

            // validate job title
            if (StringUtils.isBlank(jobTitle)) {
                mf.addError(lf.localize("DASHBOARD_INVALID_JOB_TITLE"))
                return new HashMap<String, Object>()
            }

            // validate duration
            if (years == null || years < 0) {
                mf.addError(lf.localize("DASHBOARD_INVALID_EMPLOYMENT_DURATION"))
                return new HashMap<String, Object>()
            } else if (months == null || months < 0) {
                mf.addError(lf.localize("DASHBOARD_INVALID_EMPLOYMENT_DURATION"))
                return new HashMap<String, Object>()
            } else if (years == 0 && months == 0) {
                mf.addError(lf.localize("DASHBOARD_INVALID_EMPLOYMENT_DURATION"))
                return new HashMap<String, Object>()
            }

            // validate monthly income
            if (monthlyIncome == null || monthlyIncome < 0) {
                mf.addError(lf.localize("DASHBOARD_INVALID_MONTHLY_INCOME"))
                return new HashMap<String, Object>()
            }
        }

        // validate rent income
        if (rentIncome != null && rentIncome < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_RENT_INCOME"))
            return new HashMap<String, Object>()
        }

        // validate retirement income
        if (retirementIncome != null && retirementIncome < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_RETIREMENT_INCOME"))
            return new HashMap<String, Object>()
        }

        // validate investment income
        if (investmentIncome != null && investmentIncome < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_INVESTMENT_INCOME"))
            return new HashMap<String, Object>()
        }

        // validate bonus income
        if (bonusIncome != null && bonusIncome < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_BONUS_INCOME"))
            return new HashMap<String, Object>()
        }

        // validate social security income
        if (socialSecurityIncome != null && socialSecurityIncome < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_SOCIAL_SECURITY_INCOME"))
            return new HashMap<String, Object>()
        }

        // validate disability income
        if (disabilityIncome != null && disabilityIncome < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_DISABILITY_INCOME"))
            return new HashMap<String, Object>()
        }

        // validate student income
        if (studentIncome != null && studentIncome < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_STUDENT_INCOME"))
            return new HashMap<String, Object>()
        }

        // validate unemployment income
        if (unemploymentIncome != null && unemploymentIncome < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_UNEMPLOYMENT_INCOME"))
            return new HashMap<String, Object>()
        }

        // validate alimony income
        if (alimonyIncome != null && alimonyIncome < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ALIMONY_INCOME"))
            return new HashMap<String, Object>()
        }

        // validate child support income
        if (childSupportIncome != null && childSupportIncome < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_CHILD_SUPPORT_INCOME"))
            return new HashMap<String, Object>()
        }

        // validate other income
        if (otherIncome != null && otherIncome < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_OTHER_INCOME"))
            return new HashMap<String, Object>()
        }

        // return the output parameters
        return new HashMap<>()
    }

    static Map<String, Object> addEmployment(ExecutionContext ec) {

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
        String partyId = (String) cs.getOrDefault("partyId", null)
        String occupation = (String) cs.getOrDefault("occupation", null)
        String employerName = (String) cs.getOrDefault("employerName", null)
        String jobTitle = (String) cs.getOrDefault("jobTitle", null)
        Integer years = (Integer) cs.getOrDefault("years", null)
        Integer months = (Integer) cs.getOrDefault("months", null)
        BigDecimal monthlyIncome = (BigDecimal) cs.getOrDefault("monthlyIncome", null)
        String employerAddress1 = (String) cs.getOrDefault("employerAddress1", null)
        String employerUnitNumber = (String) cs.getOrDefault("employerUnitNumber", null)
        String employerPostalCode = (String) cs.getOrDefault("employerPostalCode", null)
        String employerCity = (String) cs.getOrDefault("employerCity", null)
        String employerStateProvinceGeoId = (String) cs.getOrDefault("employerStateProvinceGeoId", null)
        String employerContactNumber = (String) cs.getOrDefault("employerContactNumber", null)
        BigDecimal rentIncome = (BigDecimal) cs.getOrDefault("rentIncome", null)
        BigDecimal retirementIncome = (BigDecimal) cs.getOrDefault("retirementIncome", null)
        BigDecimal investmentIncome = (BigDecimal) cs.getOrDefault("investmentIncome", null)
        BigDecimal bonusIncome = (BigDecimal) cs.getOrDefault("bonusIncome", null)
        BigDecimal socialSecurityIncome = (BigDecimal) cs.getOrDefault("socialSecurityIncome", null)
        BigDecimal disabilityIncome = (BigDecimal) cs.getOrDefault("disabilityIncome", null)
        BigDecimal studentIncome = (BigDecimal) cs.getOrDefault("studentIncome", null)
        BigDecimal unemploymentIncome = (BigDecimal) cs.getOrDefault("unemploymentIncome", null)
        BigDecimal alimonyIncome = (BigDecimal) cs.getOrDefault("alimonyIncome", null)
        BigDecimal childSupportIncome = (BigDecimal) cs.getOrDefault("childSupportIncome", null)
        BigDecimal otherIncome = (BigDecimal) cs.getOrDefault("otherIncome", null)

        // validate fields
        sf.sync().name("mkdecision.dashboard.OrderServices.validate#EmploymentFields")
                .parameters(cs)
                .call()
        if (mf.hasError()) {
            return new HashMap<String, Object>()
        }

        // update person
        sf.sync().name("update#mantle.party.Person")
                .parameter("partyId", partyId)
                .parameter("occupation", occupation)
                .call()

        // delete non-employment incomes
        EntityList financialFlowList = ef.find("mk.close.FinancialFlow")
                .condition("partyId", partyId)
                .condition("entryTypeEnumId", "MkEntryIncome")
                .condition("financialFlowTypeEnumId", EntityCondition.ComparisonOperator.NOT_EQUAL, "MkFinFlowTotalMonthlyIncome")
                .list()
        for (EntityValue financialFlow : financialFlowList) {
            sf.sync().name("delete#mk.close.FinancialFlow")
                    .parameter("financialFlowId", financialFlow.getString("financialFlowId"))
                    .call()
        }

        // create employer
        if (StringUtils.isNotBlank(employerName)) {

            // create employer
            Map<String, Object> employerResp = sf.sync().name("mantle.party.PartyServices.create#Organization")
                    .parameter("partyTypeEnumId", "PtyOrganization")
                    .parameter("organizationName", employerName)
                    .parameter("roleTypeId", "OrgEmployer")
                    .call()
            String employerPartyId = (String) employerResp.get("partyId")

            // create employer telecom number
            if (StringUtils.isNotBlank(employerContactNumber)) {
                sf.sync().name("mantle.party.ContactServices.create#TelecomNumber")
                        .parameter("partyId", employerPartyId)
                        .parameter("contactNumber", employerContactNumber)
                        .parameter("contactMechPurposeId", "PhonePrimary")
                        .call()
            }

            // create employer postal address
            if (StringUtils.isNotBlank(employerAddress1)) {
                sf.sync().name("mantle.party.ContactServices.create#PostalAddress")
                        .parameter("partyId", employerPartyId)
                        .parameter("address1", employerAddress1)
                        .parameter("unitNumber", employerUnitNumber)
                        .parameter("city", employerCity)
                        .parameter("postalCode", employerPostalCode)
                        .parameter("stateProvinceGeoId", employerStateProvinceGeoId)
                        .parameter("contactMechPurposeId", "PostalPrimary")
                        .call()
            }

            // calculate employment start date
            Date employmentStartDate = new Date()
            employmentStartDate = DateUtils.addYears(employmentStartDate, -years)
            employmentStartDate = DateUtils.addMonths(employmentStartDate, -months)

            // TODO: set correct relationship dates
            // TODO: set correct relationship type (current vs previous employment)

            // create employment relation
            Map<String, Object> employmentRelationshipResp = sf.sync().name("create#mantle.party.PartyRelationship")
                    .parameter("relationshipTypeEnumId", "PrtEmployee")
                    .parameter("fromPartyId", partyId)
                    .parameter("fromRoleTypeId", "Employee")
                    .parameter("toPartyId", employerPartyId)
                    .parameter("toRoleTypeId", "OrgEmployer")
                    .parameter("fromDate", employmentStartDate)
                    .parameter("relationshipName", jobTitle)
                    .call()
            String employmentRelationshipId = employmentRelationshipResp.get("partyRelationshipId")

            // create monthly income
            sf.sync().name("create#mk.close.FinancialFlow")
                    .parameter("partyId", partyId)
                    .parameter("entryTypeEnumId", "MkEntryIncome")
                    .parameter("financialFlowTypeEnumId", "MkFinFlowTotalMonthlyIncome")
                    .parameter("partyRelationshipId", employmentRelationshipId)
                    .parameter("amount", monthlyIncome)
                    .call()
        }

        // create rent income
        if (rentIncome != null) {
            sf.sync().name("create#mk.close.FinancialFlow")
                    .parameter("partyId", partyId)
                    .parameter("entryTypeEnumId", "MkEntryIncome")
                    .parameter("financialFlowTypeEnumId", "MkFinFlowRent")
                    .parameter("amount", rentIncome)
                    .call()
        }

        // create retirement income
        if (retirementIncome != null) {
            sf.sync().name("create#mk.close.FinancialFlow")
                    .parameter("partyId", partyId)
                    .parameter("entryTypeEnumId", "MkEntryIncome")
                    .parameter("financialFlowTypeEnumId", "MkFinFlowRetirement")
                    .parameter("amount", retirementIncome)
                    .call()
        }

        // create investment income
        if (investmentIncome != null) {
            sf.sync().name("create#mk.close.FinancialFlow")
                    .parameter("partyId", partyId)
                    .parameter("entryTypeEnumId", "MkEntryIncome")
                    .parameter("financialFlowTypeEnumId", "MkFinFlowInvestment")
                    .parameter("amount", investmentIncome)
                    .call()
        }

        // create bonus income
        if (bonusIncome != null) {
            sf.sync().name("create#mk.close.FinancialFlow")
                    .parameter("partyId", partyId)
                    .parameter("entryTypeEnumId", "MkEntryIncome")
                    .parameter("financialFlowTypeEnumId", "MkFinFlowBonus")
                    .parameter("amount", bonusIncome)
                    .call()
        }

        // create social security income
        if (socialSecurityIncome != null) {
            sf.sync().name("create#mk.close.FinancialFlow")
                    .parameter("partyId", partyId)
                    .parameter("entryTypeEnumId", "MkEntryIncome")
                    .parameter("financialFlowTypeEnumId", "MkFinFlowSocialSecurity")
                    .parameter("amount", socialSecurityIncome)
                    .call()
        }

        // create disability income
        if (disabilityIncome != null) {
            sf.sync().name("create#mk.close.FinancialFlow")
                    .parameter("partyId", partyId)
                    .parameter("entryTypeEnumId", "MkEntryIncome")
                    .parameter("financialFlowTypeEnumId", "MkFinFlowDisability")
                    .parameter("amount", disabilityIncome)
                    .call()
        }

        // create student income
        if (studentIncome != null) {
            sf.sync().name("create#mk.close.FinancialFlow")
                    .parameter("partyId", partyId)
                    .parameter("entryTypeEnumId", "MkEntryIncome")
                    .parameter("financialFlowTypeEnumId", "MkFinFlowStudent")
                    .parameter("amount", studentIncome)
                    .call()
        }

        // create unemployment income
        if (unemploymentIncome != null) {
            sf.sync().name("create#mk.close.FinancialFlow")
                    .parameter("partyId", partyId)
                    .parameter("entryTypeEnumId", "MkEntryIncome")
                    .parameter("financialFlowTypeEnumId", "MkFinFlowUnemployment")
                    .parameter("amount", unemploymentIncome)
                    .call()
        }

        // create alimony income
        if (alimonyIncome != null) {
            sf.sync().name("create#mk.close.FinancialFlow")
                    .parameter("partyId", partyId)
                    .parameter("entryTypeEnumId", "MkEntryIncome")
                    .parameter("financialFlowTypeEnumId", "MkFinFlowAlimony")
                    .parameter("amount", alimonyIncome)
                    .call()
        }

        // create child support income
        if (childSupportIncome != null) {
            sf.sync().name("create#mk.close.FinancialFlow")
                    .parameter("partyId", partyId)
                    .parameter("entryTypeEnumId", "MkEntryIncome")
                    .parameter("financialFlowTypeEnumId", "MkFinFlowChildSupport")
                    .parameter("amount", childSupportIncome)
                    .call()
        }

        // create other income
        if (otherIncome != null) {
            sf.sync().name("create#mk.close.FinancialFlow")
                    .parameter("partyId", partyId)
                    .parameter("entryTypeEnumId", "MkEntryIncome")
                    .parameter("financialFlowTypeEnumId", "MkFinFlowOther")
                    .parameter("amount", otherIncome)
                    .call()
        }

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("partyId", partyId)
        return outParams
    }

    static Map<String, Object> deleteEmployment(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        ServiceFacade sf = ec.getService()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)
        String partyRelationshipId = (String) cs.getOrDefault("partyRelationshipId", null)

        // find relationship
        EntityValue relationship = ef.find("mantle.party.PartyRelationship")
                .condition("partyRelationshipId", partyRelationshipId)
                .one()

        // validate relationship
        if (relationship == null || !StringUtils.equals(partyId, relationship.getString("fromPartyId")) || !StringUtils.equals(relationship.getString("relationshipTypeEnumId"), "PrtEmployee")) {
            mf.addError(lf.localize("DASHBOARD_INVALID_EMPLOYMENT"))
            return new HashMap<String, Object>()
        }

        // TODO: Cleanup employment?

        // delete relationship
        sf.sync().name("delete#mantle.party.PartyRelationship")
                .parameter("partyRelationshipId", partyRelationshipId)
                .call()

        // return the output parameters
        return new HashMap<>()
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

    static Map<String, Object> validatePropertyFields(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String orderId = (String) cs.getOrDefault("orderId", null)
        String orderPartSeqId = (String) cs.getOrDefault("orderPartSeqId", null)
        String partyId = (String) cs.getOrDefault("partyId", null)
        String assetId = (String) cs.getOrDefault("assetId", null)
        String classEnumId = (String) cs.getOrDefault("classEnumId", null)
        BigDecimal salvageValue = (BigDecimal) cs.getOrDefault("salvageValue", null)
        BigDecimal acquireCost = (BigDecimal) cs.getOrDefault("acquireCost", null)
        BigDecimal hoaFeeMonthly = (BigDecimal) cs.getOrDefault("hoaFeeMonthly", null)
        BigDecimal propertyTaxesAnnually = (BigDecimal) cs.getOrDefault("propertyTaxesAnnually", null)
        BigDecimal propertyInsuranceCostsAnnually = (BigDecimal) cs.getOrDefault("propertyInsuranceCostsAnnually", null)
        String lenderName = (String) cs.getOrDefault("lenderName", null)
        BigDecimal mortgageBalance = (BigDecimal) cs.getOrDefault("mortgageBalance", null)
        BigDecimal mortgagePaymentMonthly = (BigDecimal) cs.getOrDefault("mortgagePaymentMonthly", null)

        // validate asset class
        if (StringUtils.isBlank(classEnumId)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ASSET_CLASS"))
            return new HashMap<String, Object>()
        }

        // validate salvage value
        if (salvageValue == null || salvageValue <= 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_SALVAGE_VALUE"))
            return new HashMap<String, Object>()
        }

        // validate acquire cost
        if (acquireCost == null || acquireCost < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ACQUIRE_COST"))
            return new HashMap<String, Object>()
        }

        // validate HOA monthly fee
        if (hoaFeeMonthly == null || hoaFeeMonthly < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_HOA_FEE_MONTHLY"))
            return new HashMap<String, Object>()
        }

        // validate property tax annually
        if (propertyTaxesAnnually == null || propertyTaxesAnnually < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_PROPERTY_TAX_ANNUALLY"))
            return new HashMap<String, Object>()
        }

        // validate property insurance cost annually
        if (propertyInsuranceCostsAnnually == null || propertyInsuranceCostsAnnually < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_PROPERTY_INSURANCE_COST_ANNUALLY"))
            return new HashMap<String, Object>()
        }

        // validate lender name
        if (StringUtils.isBlank(lenderName) && (mortgageBalance != null || mortgagePaymentMonthly != null)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_LENDER_NAME"))
            return new HashMap<String, Object>()
        }

        // validate mortgage balance
        if (mortgageBalance != null && mortgageBalance <= 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_MORTGAGE_BALANCE"))
            return new HashMap<String, Object>()
        }

        // validate mortgage payment monthly
        if (mortgagePaymentMonthly != null && mortgagePaymentMonthly <= 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_MORTGAGE_PAYMENT_MONTHLY"))
            return new HashMap<String, Object>()
        }

        // return the output parameters
        return new HashMap<>()
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
        BigDecimal propertyTaxesAnnually = (BigDecimal) cs.getOrDefault("propertyTaxesAnnually", null)
        BigDecimal propertyInsuranceCostsAnnually = (BigDecimal) cs.getOrDefault("propertyInsuranceCostsAnnually", null)
        String lenderName = (String) cs.getOrDefault("lenderName", null)
        BigDecimal mortgageBalance = (BigDecimal) cs.getOrDefault("mortgageBalance", null)
        BigDecimal mortgagePaymentMonthly = (BigDecimal) cs.getOrDefault("mortgagePaymentMonthly", null)

        // validate fields
        sf.sync().name("mkdecision.dashboard.OrderServices.validate#PropertyFields")
                .parameters(cs)
                .call()
        if (mf.hasError()) {
            return new HashMap<String, Object>()
        }

        // store asset
        boolean updateAsset = StringUtils.isNotBlank(assetId);
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

        // create annual property taxes
        sf.sync().name("create#mk.close.FinancialFlow")
                .parameter("partyId", partyId)
                .parameter("entryTypeEnumId", "MkEntryExpense")
                .parameter("financialFlowTypeEnumId", "MkFinFlowAnnualPropertyTaxes")
                .parameter("assetId", assetId)
                .parameter("amount", propertyTaxesAnnually)
                .call()

        // create annual insurance costs
        sf.sync().name("create#mk.close.FinancialFlow")
                .parameter("partyId", partyId)
                .parameter("entryTypeEnumId", "MkEntryExpense")
                .parameter("financialFlowTypeEnumId", "MkFinFlowAnnualInsuranceCosts")
                .parameter("assetId", assetId)
                .parameter("amount", propertyInsuranceCostsAnnually)
                .call()

        // create lender
        if (StringUtils.isNotBlank(lenderName)) {

            // create lender
            Map<String, Object> lenderResp = sf.sync().name("mantle.party.PartyServices.create#Organization")
                    .parameter("partyTypeEnumId", "PtyOrganization")
                    .parameter("organizationName", lenderName)
                    .parameter("roleTypeId", "Lender")
                    .call()
            String lenderPartyId = (String) lenderResp.get("partyId")

            // create lender relation
            Map<String, Object> lenderRelationshipResp = sf.sync().name("create#mantle.party.PartyRelationship")
                    .parameter("relationshipTypeEnumId", "PrtMortgage")
                    .parameter("fromPartyId", partyId)
                    .parameter("fromRoleTypeId", "Borrower")
                    .parameter("toPartyId", lenderPartyId)
                    .parameter("toRoleTypeId", "Lender")
                    .parameter("fromDate", uf.getNowTimestamp())
                    .call()
            String lenderRelationshipId = lenderRelationshipResp.get("partyRelationshipId")

            // create mortgage
            sf.sync().name("create#mk.close.FinancialFlow")
                    .parameter("partyId", partyId)
                    .parameter("entryTypeEnumId", "MkEntryExpense")
                    .parameter("financialFlowTypeEnumId", "MkFinFlowMortgage")
                    .parameter("partyRelationshipId", lenderRelationshipId)
                    .parameter("balance", mortgageBalance)
                    .parameter("amount", mortgagePaymentMonthly)
                    .call()
        }

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("assetId", assetId)
        return outParams
    }

    static Map<String, Object> deleteMortgage(ExecutionContext ec) {

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
        String partyRelationshipId = (String) cs.getOrDefault("partyRelationshipId", null)

        // find relationship
        EntityValue relationship = ef.find("mantle.party.PartyRelationship")
                .condition("partyRelationshipId", partyRelationshipId)
                .one()

        // validate relationship
        if (relationship == null || !StringUtils.equals(partyId, relationship.getString("fromPartyId")) || !StringUtils.equals(relationship.getString("relationshipTypeEnumId"), "PrtMortgage")) {
            mf.addError(lf.localize("DASHBOARD_INVALID_MORTGAGE"))
            return new HashMap<String, Object>()
        }

        // TODO: Cleanup mortgage?

        // delete relationship
        sf.sync().name("delete#mantle.party.PartyRelationship")
                .parameter("partyRelationshipId", partyRelationshipId)
                .call()

        // return the output parameters
        return new HashMap<>()
    }

    static Map<String, Object> validateOrderItemFields(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        UserFacade uf = ec.getUser()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String orderId = (String) cs.getOrDefault("orderId", null)
        String orderPartSeqId = (String) cs.getOrDefault("orderPartSeqId", null)
        String productCategoryId = (String) cs.getOrDefault("productCategoryId", null)
        String productId = (String) cs.getOrDefault("productId", null)
        BigDecimal totalPurchaseAmount = (BigDecimal) cs.getOrDefault("totalPurchaseAmount", null)
        BigDecimal downPayment = (BigDecimal) cs.getOrDefault("downPayment", null)
        BigDecimal netPurchaseAmount = (BigDecimal) cs.getOrDefault("netPurchaseAmount", null)
        BigDecimal loanFee = (BigDecimal) cs.getOrDefault("loanFee", null)
        String financedAmount = (String) cs.getOrDefault("financedAmount", null)
        BigDecimal estimatedPayment = (BigDecimal) cs.getOrDefault("estimatedPayment", null)

        // validate order header
        EntityValue orderHeader = ef.find("mantle.order.OrderHeader")
                .condition("orderId", orderId)
                .one()
        if (orderHeader == null) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ORDER"))
            return new HashMap<String, Object>()
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
            mf.addError(lf.localize("DASHBOARD_INVALID_PRODUCT_CATEGORY"))
            return new HashMap<String, Object>()
        }

        // validate product category member
        long categoryMemberCount = ef.find("mantle.product.category.ProductCategoryMember")
                .condition("productCategoryId", productCategoryId)
                .condition("productId", productId)
                .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                .count()
        if (categoryMemberCount == 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_PRODUCT"))
            return new HashMap<String, Object>()
        }

        // validate total purchase amount
        if (totalPurchaseAmount == null || totalPurchaseAmount <= 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_TOTAL_PURCHASE_AMOUNT"))
            return new HashMap<String, Object>()
        }

        // validate down payment
        if (downPayment == null || downPayment < 0 || downPayment > totalPurchaseAmount) {
            mf.addError(lf.localize("DASHBOARD_INVALID_DOWN_PAYMENT"))
            return new HashMap<String, Object>()
        }

        // validate net purchase amount
        if (netPurchaseAmount == null || netPurchaseAmount != (totalPurchaseAmount - downPayment)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_NET_PURCHASE_AMOUNT"))
            return new HashMap<String, Object>()
        }

        // validate loan fee
        if (loanFee == null || loanFee < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_LOAN_FEE"))
            return new HashMap<String, Object>()
        }

        // validate financed amount
        BigDecimal financedAmountBigDecimal = new BigDecimal(financedAmount)
        if (financedAmountBigDecimal == null || financedAmountBigDecimal != ((totalPurchaseAmount + loanFee) - downPayment)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_FINANCED_AMOUNT"))
            return new HashMap<String, Object>()
        }

        // validate estimated amount
        if (estimatedPayment == null || estimatedPayment <= 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ESTIMATED_AMOUNT"))
            return new HashMap<String, Object>()
        }

        // return the output parameters
        return new HashMap<>()
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
