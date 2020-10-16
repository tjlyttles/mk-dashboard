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

import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
        BigDecimal totalPurchasePrice = (BigDecimal) cs.getOrDefault("totalPurchasePrice", null)
        BigDecimal downPayment = (BigDecimal) cs.getOrDefault("downPayment", null)
        BigDecimal loanFee = (BigDecimal) cs.getOrDefault("loanFee", null)
        String amount = (String) cs.getOrDefault("amount", null)
        BigDecimal estimatedAmount = (BigDecimal) cs.getOrDefault("estimatedAmount", null)

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

        // validate total purchase price
        if (totalPurchasePrice == null || totalPurchasePrice <= 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_TOTAL_PURCHASE_PRICE"))
            return new HashMap<String, Object>()
        }

        // validate down payment
        if (downPayment == null || downPayment < 0 || downPayment > totalPurchasePrice) {
            mf.addError(lf.localize("DASHBOARD_INVALID_DOWN_PAYMENT"))
            return new HashMap<String, Object>()
        }

        // validate loan fee
        if (loanFee == null || loanFee < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_LOAN_FEE"))
            return new HashMap<String, Object>()
        }

        // validate amount
        BigDecimal amountBigDecimal = new BigDecimal(amount)
        if (amountBigDecimal == null || amountBigDecimal != ((totalPurchasePrice + loanFee) - downPayment)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_AMOUNT"))
            return new HashMap<String, Object>()
        }

        // validate estimated amount
        if (estimatedAmount == null || estimatedAmount <= 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ESTIMATED_AMOUNT"))
            return new HashMap<String, Object>()
        }

        // return the output parameters
        return new HashMap<>()
    }

    static Map<String, Object> validatePrimaryApplicantFields(ExecutionContext ec) {

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
        String birthDate = (String) cs.getOrDefault("birthDate", null)
        String idTypeEnumId = (String) cs.getOrDefault("idTypeEnumId", null)
        String idIssuedBy = (String) cs.getOrDefault("idIssuedBy", null)
        String idValue = (String) cs.getOrDefault("idValue", null)
        String idIssueDate = (String) cs.getOrDefault("idIssueDate", null)
        String idExpiryDate = (String) cs.getOrDefault("idExpiryDate", null)
        String maritalStatusEnumId = (String) cs.getOrDefault("maritalStatusEnumId", null)
        String contactNumber = (String) cs.getOrDefault("contactNumber", null)
        String contactMechPurposeId = (String) cs.getOrDefault("contactMechPurposeId", null)
        String email = (String) cs.getOrDefault("email", null)
        String emailVerify = (String) cs.getOrDefault("emailVerify", null)
        String occupation = (String) cs.getOrDefault("occupation", null)
        String employmentStatusEnumId = (String) cs.getOrDefault("employmentStatusEnumId", null)
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
        BigDecimal otherMonthlyIncome = (BigDecimal) cs.getOrDefault("otherMonthlyIncome", null)

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

        // validate city
        if (StringUtils.isBlank(city)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_CITY"))
            return new HashMap<String, Object>()
        }

        // validate postal code
        if (StringUtils.isBlank(postalCode)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_POSTAL_CODE"))
            return new HashMap<String, Object>()
        }

        // validate social security number
        if (StringUtils.isBlank(socialSecurityNumber)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_SSN"))
            return new HashMap<String, Object>()
        }

        // validate date of birth
        Date dob = lf.parseDate(birthDate, "MM-dd-yyyy")
        Date minDob = DateUtils.addYears(new Date(), -18)
        if (dob == null) {
            mf.addError(lf.localize("DASHBOARD_INVALID_DOB"))
            return new HashMap<String, Object>()
        } else if (dob.after(minDob)) {
            mf.addError(lf.localize("DASHBOARD_APPLICANT_NOT_ELIGIBLE"))
            return new HashMap<String, Object>()
        }

        // validate contact number
        if (StringUtils.isBlank(contactNumber)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_PHONE_NUMBER"))
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
        String formResponseId = (String) cs.getOrDefault("formResponseId", null)
        BigDecimal totalPurchasePrice = (BigDecimal) cs.getOrDefault("totalPurchasePrice", null)
        BigDecimal downPayment = (BigDecimal) cs.getOrDefault("downPayment", null)
        BigDecimal loanFee = (BigDecimal) cs.getOrDefault("loanFee", null)
        BigDecimal amount = (BigDecimal) cs.getOrDefault("amount", null)
        BigDecimal estimatedAmount = (BigDecimal) cs.getOrDefault("estimatedAmount", null)

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
                .call()

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
                .parameter("productParameterId", "EstimatedPayment")
                .parameter("productParameterSetId", productParameterSetId)
                .parameter("parameterValue", estimatedAmount)
                .call()

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("orderId", orderId)
        outParams.put("orderPartSeqId", orderPartSeqId)
        return outParams
    }

    static Map<String, Object> createPrimaryApplicant(ExecutionContext ec) {

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
        String birthDate = (String) cs.getOrDefault("birthDate", null)
        String idTypeEnumId = (String) cs.getOrDefault("idTypeEnumId", null)
        String idIssuedBy = (String) cs.getOrDefault("idIssuedBy", null)
        String idValue = (String) cs.getOrDefault("idValue", null)
        String idIssueDate = (String) cs.getOrDefault("idIssueDate", null)
        String idExpiryDate = (String) cs.getOrDefault("idExpiryDate", null)
        String maritalStatusEnumId = (String) cs.getOrDefault("maritalStatusEnumId", null)
        String contactNumber = (String) cs.getOrDefault("contactNumber", null)
        String contactMechPurposeId = (String) cs.getOrDefault("contactMechPurposeId", null)
        String email = (String) cs.getOrDefault("email", null)
        String emailVerify = (String) cs.getOrDefault("emailVerify", null)
        String occupation = (String) cs.getOrDefault("occupation", null)
        String employmentStatusEnumId = (String) cs.getOrDefault("employmentStatusEnumId", null)
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
        BigDecimal otherMonthlyIncome = (BigDecimal) cs.getOrDefault("otherMonthlyIncome", null)

        // validate fields
        sf.sync().name("mkdecision.dashboard.OrderServices.validate#PrimaryApplicantFields")
                .parameters(cs)
                .call()
        if (mf.hasError()) {
            return new HashMap<String, Object>()
        }

        // create person
        Map<String, Object> personResp = sf.sync().name("mantle.party.PartyServices.create#Person")
                .parameter("partyTypeEnumId", "PtyPerson")
                .parameter("firstName", firstName)
                .parameter("middleName", middleName)
                .parameter("lastName", lastName)
                .parameter("suffix", suffix)
                .parameter("birthDate", lf.parseDate(birthDate, "MM-dd-yyyy"))
                .parameter("maritalStatusEnumId", maritalStatusEnumId)
                .parameter("employmentStatusEnumId", employmentStatusEnumId)
                .parameter("occupation", occupation)
                .call()
        String partyId = (String) personResp.get("partyId")

        // create postal address
        sf.sync().name("mantle.party.ContactServices.create#PostalAddress")
                .parameter("partyId", partyId)
                .parameter("address1", address1)
                .parameter("unitNumber", unitNumber)
                .parameter("city", city)
                .parameter("postalCode", postalCode)
                .parameter("stateProvinceGeoId", stateProvinceGeoId)
                .parameter("contactMechPurposeId", "PostalHome")
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

        // create social security number
        sf.sync().name("create#mantle.party.PartyIdentification")
                .parameter("partyId", partyId)
                .parameter("partyIdTypeEnumId", "PtidSsn")
                .parameter("idValue", socialSecurityNumber)
                .call()

        // create identification
        if (StringUtils.isNotBlank(idValue)) {
            sf.sync().name("create#mantle.party.PartyIdentification")
                    .parameter("partyId", partyId)
                    .parameter("partyIdTypeEnumId", idTypeEnumId)
                    .parameter("idValue", idValue)
                    .parameter("issuedBy", idIssuedBy)
                    .parameter("issueDate", lf.parseDate(idIssueDate, "MM-dd-yyyy"))
                    .parameter("expireDate", lf.parseDate(idExpiryDate, "MM-dd-yyyy"))
                    .call()
        }

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

        // create other monthly income
        sf.sync().name("create#mk.close.FinancialFlow")
                .parameter("partyId", partyId)
                .parameter("entryTypeEnumId", "MkEntryIncome")
                .parameter("financialFlowTypeEnumId", "MkFinFlowOther")
                .parameter("amount", otherMonthlyIncome)
                .call()

        // update order part party
        sf.sync().name("update#mantle.order.OrderPart")
                .parameter("orderId", orderId)
                .parameter("orderPartSeqId", orderPartSeqId)
                .parameter("customerPartyId", partyId)
                .call()

        // create order party
        sf.sync().name("create#mantle.order.OrderPartParty")
                .parameter("orderId", orderId)
                .parameter("orderPartSeqId", orderPartSeqId)
                .parameter("partyId", partyId)
                .parameter("roleTypeId", "PrimaryApplicant")
                .call()

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("partyId", partyId)
        return outParams
    }

    static Map<String, Object> createProperty(ExecutionContext ec) {

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
        String assetClassEnumId = (String) cs.getOrDefault("assetClassEnumId", null)
        BigDecimal salvageValue = (BigDecimal) cs.getOrDefault("salvageValue", null)
        BigDecimal acquireCost = (BigDecimal) cs.getOrDefault("acquireCost", null)
        BigDecimal hoaFeeMonthly = (BigDecimal) cs.getOrDefault("hoaFeeMonthly", null)
        BigDecimal propertyTaxesAnnually = (BigDecimal) cs.getOrDefault("propertyTaxesAnnually", null)
        BigDecimal propertyInsuranceCostsAnnually = (BigDecimal) cs.getOrDefault("propertyInsuranceCostsAnnually", null)
        String lenderName = (String) cs.getOrDefault("lenderName", null)
        BigDecimal mortgageBalance = (BigDecimal) cs.getOrDefault("mortgageBalance", null)
        BigDecimal mortgagePaymentMonthly = (BigDecimal) cs.getOrDefault("mortgagePaymentMonthly", null)

        // TODO: Add validations

        // create asset
        Map<String, Object> assetResp = sf.sync().name("create#mantle.product.asset.Asset")
                .parameter("assetTypeEnumId", "AstTpRealEstate")
                .parameter("classEnumId", assetClassEnumId)
                .parameter("salvageValue", salvageValue)
                .parameter("acquireCost", acquireCost)
                .parameter("ownerPartyId", partyId)
                .call()
        String assetId = (String) assetResp.get("assetId")

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

        // create annual property taxes
        sf.sync().name("create#mk.close.FinancialFlow")
                .parameter("partyId", partyId)
                .parameter("entryTypeEnumId", "MkEntryExpense")
                .parameter("financialFlowTypeEnumId", "MkFinFlowAnnualInsuranceCosts")
                .parameter("assetId", assetId)
                .parameter("amount", propertyInsuranceCostsAnnually)
                .call()

        // create lender
        Map<String, Object> lenderResp = sf.sync().name("mantle.party.PartyServices.create#Organization")
                .parameter("partyTypeEnumId", "PtyOrganization")
                .parameter("organizationName", lenderName)
                .parameter("roleTypeId", "Lender")
                .call()
        String lenderPartyId = (String) lenderResp.get("partyId")

        // create lender relation
        Map<String, Object> lenderRelationshipResp = sf.sync().name("create#mantle.party.PartyRelationship")
                .parameter("relationshipTypeEnumId", "PrtEmployee")
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

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("assetId", assetId)
        return outParams
    }
}
