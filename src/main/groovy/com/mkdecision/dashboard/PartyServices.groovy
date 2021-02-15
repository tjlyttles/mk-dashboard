package com.mkdecision.dashboard

import org.apache.commons.lang3.time.DateUtils
import org.moqui.context.*
import org.moqui.entity.*
import org.moqui.service.ServiceFacade
import org.moqui.util.*
import org.apache.commons.lang3.StringUtils

class PartyServices {

    static Map<String, Object> getPartyName(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)
        Boolean includeMiddleName = (Boolean) cs.getOrDefault("includeMiddleName", false)
        Boolean includeSuffix = (Boolean) cs.getOrDefault("includeSuffix", false)

        // get the party
        EntityValue partyDetail = ef.find("mantle.party.PartyDetail")
                .condition("partyId", partyId)
                .one()

        // prepare party name
        String partyName = null
        String partyNickname = null
        if (partyDetail != null) {
            if (partyDetail.getString("partyTypeEnumId") == "PtyPerson") {
                String firstName = partyDetail.getString("firstName")
                String middleName = partyDetail.getString("middleName")
                String lastName = partyDetail.getString("lastName")
                String suffix = partyDetail.getString("suffix")

                ArrayList<String> partyNameParts = new ArrayList<>()
                if (StringUtils.isNotBlank(firstName)) {
                    partyNameParts.add(firstName)
                }
                if (includeMiddleName && StringUtils.isNotBlank(middleName)) {
                    partyNameParts.add(middleName)
                }
                if (StringUtils.isNotBlank(lastName)) {
                    partyNameParts.add(lastName)
                }
                if (includeSuffix && StringUtils.isNotBlank(suffix)) {
                    partyNameParts.add(suffix)
                }

                partyName = StringUtils.defaultIfBlank(StringUtils.join(partyNameParts, " "), null)
                partyNickname = partyDetail.getString("nickname")
            } else if (partyDetail.getString("partyTypeEnumId") == "PtyOrganization") {
                partyName = partyDetail.getString("organizationName")
            }
        }

        // return the output parameters
        HashMap<String, Object> outParams = new HashMap<>()
        outParams.put("partyId", partyId)
        outParams.put("partyName", partyName)
        outParams.put("partyNickname", partyNickname)
        return outParams
    }

    static Map<String, Object> getSocialSecurityNumber(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        UserFacade uf = ec.getUser()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)

        // get the social security number
        EntityValue ssnId = ef.find("mantle.party.PartyIdentification")
                .condition("partyId", partyId)
                .condition("partyIdTypeEnumId", "PtidSsn")
                .one()

        // prepare social security number
        String socialSecurityNumber = ssnId == null ? null : ssnId.getString("idValue")

        // return the output parameters
        HashMap<String, Object> outParams = new HashMap<>()
        outParams.put("partyId", partyId)
        outParams.put("socialSecurityNumber", socialSecurityNumber)
        return outParams
    }

    static Map<String, Object> getPrimaryEmailAddress(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        UserFacade uf = ec.getUser()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)

        // get the telecom numbers
        EntityList emails = ef.find("mantle.party.contact.PartyContactMechInfo")
                .condition("partyId", partyId)
                .condition("contactMechPurposeId", "EmailPrimary")
                .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                .list()

        // prepare email address
        String emailAddress = null
        if (emails != null && !emails.isEmpty()) {
            EntityValue firstEmail = emails.getFirst()
            emailAddress = firstEmail.getString("infoString")
        }

        // return the output parameters
        HashMap<String, Object> outParams = new HashMap<>()
        outParams.put("partyId", partyId)
        outParams.put("emailAddress", emailAddress)
        return outParams
    }

    static Map<String, Object> getPrimaryPostalAddress(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        UserFacade uf = ec.getUser()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)

        // get the postal addresses
        EntityList postalAddresses = ef.find("mantle.party.contact.PartyContactMechPostalAddress")
                .condition("partyId", partyId)
                .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                .list()
        EntityValue postalAddress = postalAddresses != null && !postalAddresses.isEmpty() ? postalAddresses.getFirst() : null

        // prepare postal address string
        ArrayList<String> addressParts = new ArrayList<>()
        if (postalAddress != null) {
            String address1 = postalAddress.getString("address1")
            String city = postalAddress.getString("city")
            String stateGeoCodeAlpha2 = postalAddress.getString("stateGeoCodeAlpha2")
            String postalCode = postalAddress.getString("postalCode")
            if (StringUtils.isNotBlank(address1)) addressParts.add(address1)
            if (StringUtils.isNotBlank(city)) addressParts.add(city)
            if (StringUtils.isNotBlank(stateGeoCodeAlpha2) && StringUtils.isNotBlank(postalCode)) addressParts.add(String.format("%s %s", stateGeoCodeAlpha2, postalCode))
        }
        String postalAddressString = StringUtils.defaultIfBlank(StringUtils.join(addressParts, ", "), null)

        // return the output parameters
        HashMap<String, Object> outParams = new HashMap<>()
        outParams.put("partyId", partyId)
        outParams.put("postalAddress", postalAddress)
        outParams.put("postalAddressString", postalAddressString)
        return outParams
    }

    static Map<String, Object> getPrimaryTelecomNumber(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        UserFacade uf = ec.getUser()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)
        Boolean includeAreaCode = (Boolean) cs.getOrDefault("includeAreaCode", false)

        // get the telecom numbers
        EntityList telecomNumbers = ef.find("mantle.party.contact.PartyContactMechTelecomNumber")
                .condition("partyId", partyId)
                .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                .list()
        EntityValue telecomNumber = telecomNumbers != null && !telecomNumbers.isEmpty() ? telecomNumbers.getFirst() : null

        // prepare contact number
        String contactNumber = null
        String contactMechPurposeId = null
        if (telecomNumber != null) {
            String areaCode = telecomNumber.getString("areaCode")
            String phoneNumber = telecomNumber.getString("contactNumber")
            contactNumber = includeAreaCode ? String.format("%s-%s", areaCode, phoneNumber) : phoneNumber
            contactMechPurposeId = telecomNumber.getString("contactMechPurposeId")
        }

        // return the output parameters
        HashMap<String, Object> outParams = new HashMap<>()
        outParams.put("partyId", partyId)
        outParams.put("contactMechPurposeId", contactMechPurposeId)
        outParams.put("contactNumber", contactNumber)
        return outParams
    }

    static Map<String, Object> getPartyProductStoreRoles(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        UserFacade uf = ec.getUser()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)
        String productStoreId = (String) cs.getOrDefault("productStoreId", null)

        // get the product store parties
        EntityList productStoreParties = ef.find("mantle.product.store.ProductStoreParty")
                .condition("productStoreId", productStoreId)
                .condition("partyId", partyId)
                .conditionDate("fromDate", "thruDate", uf.nowTimestamp)
                .list()

        // prepare role types
        Set<String> roleTypeIdSet = new HashSet<>()
        for (EntityValue productStoreParty : productStoreParties) {
            roleTypeIdSet.add(productStoreParty.getString("roleTypeId"))
        }

        // return the output parameters
        HashMap<String, Object> outParams = new HashMap<>()
        outParams.put("partyId", partyId)
        outParams.put("productStoreId", productStoreId)
        outParams.put("roleTypeIdSet", roleTypeIdSet)
        return outParams
    }

    static void validatePersonFields(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)
        String firstName = (String) cs.getOrDefault("firstName", null)
        String middleName = (String) cs.getOrDefault("middleName", null)
        String lastName = (String) cs.getOrDefault("lastName", null)
        String suffix = (String) cs.getOrDefault("suffix", null)
        String nickname = (String) cs.getOrDefault("nickname", null)
        String socialSecurityNumber = (String) cs.getOrDefault("socialSecurityNumber", null)
        Date birthDate = (Date) cs.getOrDefault("birthDate", null)
        String maritalStatusEnumId = (String) cs.getOrDefault("maritalStatusEnumId", null)
        String employmentStatusEnumId = (String) cs.getOrDefault("employmentStatusEnumId", null)

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
        }
    }

    static Map<String, Object> updatePerson(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        ServiceFacade sf = ec.getService()
        UserFacade uf = ec.getUser()
        MessageFacade mf = ec.getMessage()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)
        String firstName = (String) cs.getOrDefault("firstName", null)
        String middleName = (String) cs.getOrDefault("middleName", null)
        String lastName = (String) cs.getOrDefault("lastName", null)
        String suffix = (String) cs.getOrDefault("suffix", null)
        String nickname = (String) cs.getOrDefault("nickname", null)
        String socialSecurityNumber = (String) cs.getOrDefault("socialSecurityNumber", null)
        Date birthDate = (Date) cs.getOrDefault("birthDate", null)
        String maritalStatusEnumId = (String) cs.getOrDefault("maritalStatusEnumId", null)
        String employmentStatusEnumId = (String) cs.getOrDefault("employmentStatusEnumId", null)

        // validate fields
        sf.sync().name("mkdecision.dashboard.PartyServices.validate#PersonFields")
                .parameters(cs)
                .call()
        if (mf.hasError()) {
            return new HashMap<String, Object>()
        }

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

    static void validateContactFields(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)
        String address1 = (String) cs.getOrDefault("address1", null)
        String unitNumber = (String) cs.getOrDefault("unitNumber", null)
        String postalCode = (String) cs.getOrDefault("postalCode", null)
        String city = (String) cs.getOrDefault("city", null)
        String stateProvinceGeoId = (String) cs.getOrDefault("stateProvinceGeoId", null)
        Integer addressYears = (Integer) cs.getOrDefault("addressYears", 0)
        Integer addressMonths = (Integer) cs.getOrDefault("addressMonths", 0)
        String contactNumber = (String) cs.getOrDefault("contactNumber", null)
        String contactMechPurposeId = (String) cs.getOrDefault("contactMechPurposeId", null)
        String email = (String) cs.getOrDefault("email", null)
        String emailVerify = (String) cs.getOrDefault("emailVerify", null)

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
        if (StringUtils.isBlank(email)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_EMAIL"))
        } else if (!StringUtils.equals(email, emailVerify)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_EMAIL_VERIFY"))
        }
    }

    static Map<String, Object> updateContact(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        ServiceFacade sf = ec.getService()
        UserFacade uf = ec.getUser()
        MessageFacade mf = ec.getMessage()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)
        String address1 = (String) cs.getOrDefault("address1", null)
        String unitNumber = (String) cs.getOrDefault("unitNumber", null)
        String postalCode = (String) cs.getOrDefault("postalCode", null)
        String city = (String) cs.getOrDefault("city", null)
        String stateProvinceGeoId = (String) cs.getOrDefault("stateProvinceGeoId", null)
        Integer addressYears = (Integer) cs.getOrDefault("addressYears", 0)
        Integer addressMonths = (Integer) cs.getOrDefault("addressMonths", 0)
        String contactNumber = (String) cs.getOrDefault("contactNumber", null)
        String contactMechPurposeId = (String) cs.getOrDefault("contactMechPurposeId", null)
        String email = (String) cs.getOrDefault("email", null)
        String emailVerify = (String) cs.getOrDefault("emailVerify", null)

        // validate fields
        sf.sync().name("mkdecision.dashboard.PartyServices.validate#ContactFields")
                .parameters(cs)
                .call()
        if (mf.hasError()) {
            return new HashMap<String, Object>()
        }

        // calculate date for address duration
        Date usedSince = new Date()
        usedSince = DateUtils.addYears(usedSince, -addressYears)
        usedSince = DateUtils.addMonths(usedSince, -addressMonths)

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
        sf.sync().name("update#mantle.party.contact.ContactMech")
                .parameter("contactMechId", info.getString("contactMechId"))
                .parameter("infoString", email)
                .call()

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("partyId", partyId)
        return outParams
    }

    static void validateIdentityFields(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)
        String partyIdTypeEnumId = (String) cs.getOrDefault("partyIdTypeEnumId", null)
        String idIssuedBy = (String) cs.getOrDefault("idIssuedBy", null)
        String idValue = (String) cs.getOrDefault("idValue", null)
        Date idIssueDate = (Date) cs.getOrDefault("idIssueDate", null)
        Date idExpiryDate = (Date) cs.getOrDefault("idExpiryDate", null)

        // validate ID type
        if (StringUtils.isBlank(partyIdTypeEnumId)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ID_TYPE"))
            return
        }

        // validate ID value
        if (StringUtils.isBlank(idValue)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ID_VALUE"))
            return
        }

        // validate ID issued by
        if (StringUtils.isBlank(idIssuedBy)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ID_ISSUER"))
            return
        }

        // validate issue date
        if (idIssueDate == null && !StringUtils.equals(partyIdTypeEnumId, "PtidArn")) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ID_ISSUE_DATE"))
            return
        }

        // validate expiry date
        if (idExpiryDate == null) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ID_EXPIRY_DATE"))
        }
    }

    static Map<String, Object> addIdentity(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        ServiceFacade sf = ec.getService()
        MessageFacade mf = ec.getMessage()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)
        String partyIdTypeEnumId = (String) cs.getOrDefault("partyIdTypeEnumId", null)
        String idIssuedBy = (String) cs.getOrDefault("idIssuedBy", null)
        String idValue = (String) cs.getOrDefault("idValue", null)
        Date idIssueDate = (Date) cs.getOrDefault("idIssueDate", null)
        Date idExpiryDate = (Date) cs.getOrDefault("idExpiryDate", null)

        // validate fields
        sf.sync().name("mkdecision.dashboard.PartyServices.validate#IdentityFields")
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

    static Map<String, Object> updateIdentity(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        ServiceFacade sf = ec.getService()
        MessageFacade mf = ec.getMessage()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)
        String partyIdTypeEnumId = (String) cs.getOrDefault("partyIdTypeEnumId", null)
        String idIssuedBy = (String) cs.getOrDefault("idIssuedBy", null)
        String idValue = (String) cs.getOrDefault("idValue", null)
        Date idIssueDate = (Date) cs.getOrDefault("idIssueDate", null)
        Date idExpiryDate = (Date) cs.getOrDefault("idExpiryDate", null)

        // validate fields
        sf.sync().name("mkdecision.dashboard.PartyServices.validate#IdentityFields")
                .parameters(cs)
                .call()
        if (mf.hasError()) {
            return new HashMap<String, Object>()
        }

        // update identity
        sf.sync().name("update#mantle.party.PartyIdentification")
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

    static void validateEmploymentFields(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)
        String relationshipTypeEnumId = (String) cs.getOrDefault("relationshipTypeEnumId", null)
        String employerName = (String) cs.getOrDefault("employerName", null)
        String employmentStatusEnumId = (String) cs.getOrDefault("employmentStatusEnumId", null)
        String employerClassificationId = (String) cs.getOrDefault("employerClassificationId", null)
        String jobTitle = (String) cs.getOrDefault("jobTitle", null)
        Integer years = (Integer) cs.getOrDefault("years", null)
        Integer months = (Integer) cs.getOrDefault("months", null)
        Date fromDate = (Date) cs.getOrDefault("fromDate", null)
        Date toDate = (Date) cs.getOrDefault("toDate", null)
        BigDecimal monthlyIncome = (BigDecimal) cs.getOrDefault("monthlyIncome", null)
        String employerAddress1 = (String) cs.getOrDefault("employerAddress1", null)
        String employerUnitNumber = (String) cs.getOrDefault("employerUnitNumber", null)
        String employerPostalCode = (String) cs.getOrDefault("employerPostalCode", null)
        String employerCity = (String) cs.getOrDefault("employerCity", null)
        String employerStateProvinceGeoId = (String) cs.getOrDefault("employerStateProvinceGeoId", null)
        String employerContactNumber = (String) cs.getOrDefault("employerContactNumber", null)

        // validate employer name
        if (StringUtils.isBlank(employerName)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_EMPLOYER_NAME"))
            return
        }

        // validate based on employment status
        if (StringUtils.equals(employmentStatusEnumId, "EmpsFullTime") || StringUtils.equals(employmentStatusEnumId, "EmpsPartTime")) {
            if (StringUtils.isBlank(jobTitle)) {
                mf.addError(lf.localize("DASHBOARD_INVALID_JOB_TITLE"))
                return
            }
        } else if (StringUtils.equals(employmentStatusEnumId, "EmpsContractor") || StringUtils.equals(employmentStatusEnumId, "EmpsSelf")) {
            if (StringUtils.isBlank(employerClassificationId)) {
                mf.addError(lf.localize("DASHBOARD_INVALID_EMPLOYER_CLASS"))
                return
            }
        }

        // validate duration
        if (StringUtils.equals(relationshipTypeEnumId, "PrtEmployee")) {
            if (years == null || years < 0) {
                mf.addError(lf.localize("DASHBOARD_INVALID_EMPLOYMENT_DURATION"))
                return
            } else if (months == null || months < 0) {
                mf.addError(lf.localize("DASHBOARD_INVALID_EMPLOYMENT_DURATION"))
                return
            } else if (years == 0 && months == 0) {
                mf.addError(lf.localize("DASHBOARD_INVALID_EMPLOYMENT_DURATION"))
                return
            }
        } else if (StringUtils.equals(relationshipTypeEnumId, "PrtPreviousEmployee")) {
            if (fromDate == null || toDate == null) {
                mf.addError(lf.localize("DASHBOARD_INVALID_EMPLOYMENT_DURATION"))
                return
            } else if (fromDate.after(toDate)) {
                mf.addError(lf.localize("DASHBOARD_INVALID_EMPLOYMENT_DURATION"))
                return
            } else if (toDate.after(new Date())) {
                mf.addError(lf.localize("DASHBOARD_INVALID_EMPLOYMENT_DURATION"))
                return
            }
        }

        // validate monthly income
        if (monthlyIncome == null || monthlyIncome < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_MONTHLY_INCOME"))
        }
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
        String partyId = (String) cs.getOrDefault("partyId", null)
        String relationshipTypeEnumId = (String) cs.getOrDefault("relationshipTypeEnumId", null)
        String employerName = (String) cs.getOrDefault("employerName", null)
        String employmentStatusEnumId = (String) cs.getOrDefault("employmentStatusEnumId", null)
        String employerClassificationId = (String) cs.getOrDefault("employerClassificationId", null)
        String jobTitle = (String) cs.getOrDefault("jobTitle", null)
        Integer years = (Integer) cs.getOrDefault("years", null)
        Integer months = (Integer) cs.getOrDefault("months", null)
        Date fromDate = (Date) cs.getOrDefault("fromDate", null)
        Date toDate = (Date) cs.getOrDefault("toDate", null)
        BigDecimal monthlyIncome = (BigDecimal) cs.getOrDefault("monthlyIncome", null)
        String employerAddress1 = (String) cs.getOrDefault("employerAddress1", null)
        String employerUnitNumber = (String) cs.getOrDefault("employerUnitNumber", null)
        String employerPostalCode = (String) cs.getOrDefault("employerPostalCode", null)
        String employerCity = (String) cs.getOrDefault("employerCity", null)
        String employerStateProvinceGeoId = (String) cs.getOrDefault("employerStateProvinceGeoId", null)
        String employerContactNumber = (String) cs.getOrDefault("employerContactNumber", null)

        // validate fields
        sf.sync().name("mkdecision.dashboard.PartyServices.validate#EmploymentFields")
                .parameters(cs)
                .call()
        if (mf.hasError()) {
            return new HashMap<String, Object>()
        }

        // calculate from date
        if (StringUtils.equals(relationshipTypeEnumId, "PrtEmployee")) {
            fromDate = new Date()
            fromDate = DateUtils.addYears(fromDate, -years)
            fromDate = DateUtils.addMonths(fromDate, -months)
        }

        // create employer
        Map<String, Object> employerResp = sf.sync().name("mantle.party.PartyServices.create#Organization")
                .parameter("partyTypeEnumId", "PtyOrganization")
                .parameter("organizationName", employerName)
                .parameter("roleTypeId", "OrgEmployer")
                .call()
        String employerPartyId = (String) employerResp.get("partyId")

        // create employer classification
        if (StringUtils.equals(employmentStatusEnumId, "EmpsContractor") || StringUtils.equals(employmentStatusEnumId, "EmpsSelf")) {
            sf.sync().name("create#mantle.party.PartyClassificationAppl")
                    .parameter("partyId", employerPartyId)
                    .parameter("partyClassificationId", employerClassificationId)
                    .parameter("fromDate", fromDate.getTime())
                    .call()
        }

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

        // create employment relation
        Map<String, Object> employmentRelationshipResp = sf.sync().name("create#mantle.party.PartyRelationship")
                .parameter("relationshipTypeEnumId", relationshipTypeEnumId)
                .parameter("fromPartyId", partyId)
                .parameter("fromRoleTypeId", "Employee")
                .parameter("toPartyId", employerPartyId)
                .parameter("toRoleTypeId", "OrgEmployer")
                .parameter("fromDate", fromDate)
                .parameter("thruDate", toDate)
                .parameter("relationshipName", jobTitle)
                .call()
        String partyRelationshipId = employmentRelationshipResp.get("partyRelationshipId")

        // add employment status relationship setting
        sf.sync().name("create#mantle.party.PartyRelationshipSetting")
                .parameter("partyRelationshipId", partyRelationshipId)
                .parameter("partySettingTypeId", "EmploymentStatus")
                .parameter("settingValue", employmentStatusEnumId)
                .call()

        // create monthly income
        sf.sync().name("create#mk.close.FinancialFlow")
                .parameter("partyId", partyId)
                .parameter("partyRelationshipId", partyRelationshipId)
                .parameter("entryTypeEnumId", "MkEntryIncome")
                .parameter("financialFlowTypeEnumId", "MkFinFlowTotalMonthlyIncome")
                .parameter("amount", monthlyIncome)
                .parameter("fromDate", fromDate.getTime())
                .call()

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("partyId", partyId)
        outParams.put("partyRelationshipId", partyRelationshipId)
        return outParams
    }

    static Map<String, Object> updateEmployment(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        ServiceFacade sf = ec.getService()
        UserFacade uf = ec.getUser()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)
        String partyRelationshipId = (String) cs.getOrDefault("partyRelationshipId", null)
        String relationshipTypeEnumId = (String) cs.getOrDefault("relationshipTypeEnumId", null)
        String employerName = (String) cs.getOrDefault("employerName", null)
        String employmentStatusEnumId = (String) cs.getOrDefault("employmentStatusEnumId", null)
        String employerClassificationId = (String) cs.getOrDefault("employerClassificationId", null)
        String jobTitle = (String) cs.getOrDefault("jobTitle", null)
        Integer years = (Integer) cs.getOrDefault("years", null)
        Integer months = (Integer) cs.getOrDefault("months", null)
        Date fromDate = (Date) cs.getOrDefault("fromDate", null)
        Date toDate = (Date) cs.getOrDefault("toDate", null)
        BigDecimal monthlyIncome = (BigDecimal) cs.getOrDefault("monthlyIncome", null)
        String employerAddress1 = (String) cs.getOrDefault("employerAddress1", null)
        String employerUnitNumber = (String) cs.getOrDefault("employerUnitNumber", null)
        String employerPostalCode = (String) cs.getOrDefault("employerPostalCode", null)
        String employerCity = (String) cs.getOrDefault("employerCity", null)
        String employerStateProvinceGeoId = (String) cs.getOrDefault("employerStateProvinceGeoId", null)
        String employerContactNumber = (String) cs.getOrDefault("employerContactNumber", null)

        // validate fields
        sf.sync().name("mkdecision.dashboard.PartyServices.validate#EmploymentFields")
                .parameters(cs)
                .call()
        if (mf.hasError()) {
            return new HashMap<String, Object>()
        }

        // calculate from date
        if (StringUtils.equals(relationshipTypeEnumId, "PrtEmployee")) {
            fromDate = new Date()
            fromDate = DateUtils.addYears(fromDate, -years)
            fromDate = DateUtils.addMonths(fromDate, -months)
        }

        // validate relationship
        EntityValue relationship = ef.find("mantle.party.PartyRelationship")
                .condition("partyRelationshipId", partyRelationshipId)
                .one()
        if (relationship == null || !StringUtils.equals(partyId, relationship.getString("fromPartyId")) || (!StringUtils.equals(relationship.getString("relationshipTypeEnumId"), "PrtEmployee") && !StringUtils.equals(relationship.getString("relationshipTypeEnumId"), "PrtPreviousEmployee"))) {
            mf.addError(lf.localize("DASHBOARD_INVALID_EMPLOYMENT"))
            return new HashMap<String, Object>()
        }

        // update employer
        String employerPartyId = (String) relationship.get("toPartyId")
        sf.sync().name("update#mantle.party.Organization")
                .parameter("partyId", employerPartyId)
                .parameter("organizationName", employerName)
                .call()

        // update employer classification
        if (StringUtils.equals(employmentStatusEnumId, "EmpsContractor") || StringUtils.equals(employmentStatusEnumId, "EmpsSelf")) {
            EntityList partyClasses = ef.find("mantle.party.PartyClassificationAppl")
                    .condition("partyId", employerPartyId)
                    .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                    .list()
            for (EntityValue partyClass : partyClasses) {
                sf.sync().name("mantle.party.PartyServices.remove#PartyClassification")
                        .parameter("partyId", partyClass.getString("partyId"))
                        .parameter("partyClassificationId", partyClass.getString("partyClassificationId"))
                        .call()
            }
            sf.sync().name("create#mantle.party.PartyClassificationAppl")
                    .parameter("partyId", employerPartyId)
                    .parameter("partyClassificationId", employerClassificationId)
                    .parameter("fromDate", fromDate.getTime())
                    .call()
        }

        // update employer telecom number
        EntityValue telecomNumber = ef.find("mantle.party.contact.PartyContactMechTelecomNumber")
                .condition("partyId", employerPartyId)
                .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                .list()
                .getFirst()
        if (telecomNumber != null) {
            sf.sync().name("mantle.party.ContactServices.delete#PartyContactMech")
                    .parameter("partyId", employerPartyId)
                    .parameter("contactMechId", telecomNumber.getString("contactMechId"))
                    .call()
        }
        if (StringUtils.isNotBlank(employerContactNumber)) {
            sf.sync().name("mantle.party.ContactServices.create#TelecomNumber")
                    .parameter("partyId", employerPartyId)
                    .parameter("contactNumber", employerContactNumber)
                    .parameter("contactMechPurposeId", "PhonePrimary")
                    .call()
        }

        // update employer postal address
        EntityValue postalAddress = ef.find("mantle.party.contact.PartyContactMechPostalAddress")
                .condition("partyId", employerPartyId)
                .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
                .list()
                .getFirst()
        if (postalAddress != null) {
            sf.sync().name("mantle.party.ContactServices.delete#PartyContactMech")
                    .parameter("partyId", employerPartyId)
                    .parameter("contactMechId", postalAddress.getString("contactMechId"))
                    .call()
        }
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

        // update employment relation
        Map<String, Object> employmentRelationshipResp = sf.sync().name("update#mantle.party.PartyRelationship")
                .parameter("partyRelationshipId", partyRelationshipId)
                .parameter("fromDate", fromDate.getTime())
                .parameter("thruDate", toDate)
                .parameter("relationshipName", jobTitle)
                .call()

        // update employment status relationship setting
        sf.sync().name("update#mantle.party.PartyRelationshipSetting")
                .parameter("partyRelationshipId", partyRelationshipId)
                .parameter("partySettingTypeId", "EmploymentStatus")
                .parameter("settingValue", employmentStatusEnumId)
                .call()

        // update monthly income
        EntityValue monthlyIncomeFinFlow = ef.find("mk.close.FinancialFlow")
                .condition("partyId", partyId)
                .condition("partyRelationshipId", partyRelationshipId)
                .condition("entryTypeEnumId", "MkEntryIncome")
                .condition("financialFlowTypeEnumId", "MkFinFlowTotalMonthlyIncome")
                .list()
                .getFirst()
        sf.sync().name("update#mk.close.FinancialFlow")
                .parameter("financialFlowId", monthlyIncomeFinFlow.getString("financialFlowId"))
                .parameter("amount", monthlyIncome)
                .parameter("fromDate", fromDate.getTime())
                .call()

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("partyId", partyId)
        outParams.put("partyRelationshipId", partyRelationshipId)
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

        // validate relationship
        EntityValue relationship = ef.find("mantle.party.PartyRelationship")
                .condition("partyRelationshipId", partyRelationshipId)
                .one()
        if (relationship == null || !StringUtils.equals(partyId, relationship.getString("fromPartyId")) || !StringUtils.equals(relationship.getString("relationshipTypeEnumId"), "PrtEmployee")) {
            mf.addError(lf.localize("DASHBOARD_INVALID_EMPLOYMENT"))
            return new HashMap<String, Object>()
        }

        // delete monthly income
        EntityValue incomeFinancialFlow = ef.find("mk.close.FinancialFlow")
                .condition("partyId", partyId)
                .condition("entryTypeEnumId", "MkEntryIncome")
                .condition("financialFlowTypeEnumId", "MkFinFlowTotalMonthlyIncome")
                .condition("partyRelationshipId", partyRelationshipId)
                .list()
                .getFirst()
        sf.sync().name("delete#mk.close.FinancialFlow")
                .parameter("financialFlowId", incomeFinancialFlow.getString("financialFlowId"))
                .call()

        // delete relationship settings
        sf.sync().name("delete#mantle.party.PartyRelationshipSetting")
                .parameter("partyRelationshipId", partyRelationshipId)
                .parameter("partySettingTypeId", "*")
                .call()

        // delete relationship
        sf.sync().name("delete#mantle.party.PartyRelationship")
                .parameter("partyRelationshipId", partyRelationshipId)
                .call()

        // return the output parameters
        return new HashMap<>()
    }

    static void validateIncomeSourceFields(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)
        String financialFlowTypeEnumId = (String) cs.getOrDefault("financialFlowTypeEnumId", null)
        BigDecimal amount = (BigDecimal) cs.getOrDefault("amount", null)
        Integer years = (Integer) cs.getOrDefault("years", null)
        Integer months = (Integer) cs.getOrDefault("months", null)

        // validate financial flow type
        if (StringUtils.isBlank(financialFlowTypeEnumId)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_INCOME_SOURCE_TYPE"))
        }

        // validate amount
        if (amount == null || amount <= 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_INCOME_SOURCE_AMOUNT"))
        }

        // validate duration
        if (years == null || years < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_INCOME_SOURCE_DURATION"))
        } else if (months == null || months < 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_INCOME_SOURCE_DURATION"))
        } else if (years == 0 && months == 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_INCOME_SOURCE_DURATION"))
        }
    }

    static Map<String, Object> addIncomeSource(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        ServiceFacade sf = ec.getService()
        UserFacade uf = ec.getUser()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)
        String financialFlowTypeEnumId = (String) cs.getOrDefault("financialFlowTypeEnumId", null)
        BigDecimal amount = (BigDecimal) cs.getOrDefault("amount", null)
        Integer years = (Integer) cs.getOrDefault("years", null)
        Integer months = (Integer) cs.getOrDefault("months", null)

        // validate fields
        sf.sync().name("mkdecision.dashboard.PartyServices.validate#IncomeSourceFields")
                .parameters(cs)
                .call()
        if (mf.hasError()) {
            return new HashMap<String, Object>()
        }

        // create income source
        Date incomeStartDate = new Date()
        incomeStartDate = DateUtils.addYears(incomeStartDate, -years)
        incomeStartDate = DateUtils.addMonths(incomeStartDate, -months)
        Map<String, Object> finFlowResp = sf.sync().name("create#mk.close.FinancialFlow")
                .parameter("entryTypeEnumId", "MkEntryIncome")
                .parameter("financialFlowTypeEnumId", financialFlowTypeEnumId)
                .parameter("partyId", partyId)
                .parameter("amount", amount)
                .parameter("fromDate", incomeStartDate.getTime())
                .call()
        String financialFlowId = (String) finFlowResp.get("financialFlowId")

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("partyId", partyId)
        outParams.put("financialFlowId", financialFlowId)
        return outParams
    }

    static Map<String, Object> updateIncomeSource(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        ServiceFacade sf = ec.getService()
        UserFacade uf = ec.getUser()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)
        String financialFlowId = (String) cs.getOrDefault("financialFlowId", null)
        String financialFlowTypeEnumId = (String) cs.getOrDefault("financialFlowTypeEnumId", null)
        BigDecimal amount = (BigDecimal) cs.getOrDefault("amount", null)
        Integer years = (Integer) cs.getOrDefault("years", null)
        Integer months = (Integer) cs.getOrDefault("months", null)

        // validate fields
        sf.sync().name("mkdecision.dashboard.PartyServices.validate#IncomeSourceFields")
                .parameters(cs)
                .call()
        if (mf.hasError()) {
            return new HashMap<String, Object>()
        }

        // validate financial flow
        EntityValue finFlow = ef.find("mk.close.FinancialFlow")
                .condition("financialFlowId", financialFlowId)
                .one()
        if (finFlow == null || !StringUtils.equals(partyId, finFlow.getString("partyId")) || !StringUtils.equals(finFlow.getString("entryTypeEnumId"), "MkEntryIncome")) {
            mf.addError(lf.localize("DASHBOARD_INVALID_INCOME_SOURCE"))
            return new HashMap<String, Object>()
        }

        // update financial flow
        Date incomeStartDate = new Date()
        incomeStartDate = DateUtils.addYears(incomeStartDate, -years)
        incomeStartDate = DateUtils.addMonths(incomeStartDate, -months)
        Map<String, Object> finFlowResp = sf.sync().name("update#mk.close.FinancialFlow")
                .parameter("financialFlowId", financialFlowId)
                .parameter("financialFlowTypeEnumId", financialFlowTypeEnumId)
                .parameter("amount", amount)
                .parameter("fromDate", incomeStartDate.getTime())
                .call()

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("partyId", partyId)
        outParams.put("financialFlowId", financialFlowId)
        return outParams
    }

    static Map<String, Object> deleteIncomeSource(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        ServiceFacade sf = ec.getService()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)
        String financialFlowId = (String) cs.getOrDefault("financialFlowId", null)

        // validate financial flow
        EntityValue finFlow = ef.find("mk.close.FinancialFlow")
                .condition("financialFlowId", financialFlowId)
                .one()
        if (finFlow == null || !StringUtils.equals(partyId, finFlow.getString("partyId")) || !StringUtils.equals(finFlow.getString("entryTypeEnumId"), "MkEntryIncome")) {
            mf.addError(lf.localize("DASHBOARD_INVALID_INCOME_SOURCE"))
            return new HashMap<String, Object>()
        }

        // delete financial flow
        sf.sync().name("delete#mk.close.FinancialFlow")
                .parameter("financialFlowId", financialFlowId)
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
        }
    }

    static Map<String, Object> updateProperty(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        ServiceFacade sf = ec.getService()
        MessageFacade mf = ec.getMessage()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)
        String assetId = (String) cs.getOrDefault("assetId", null)
        String classEnumId = (String) cs.getOrDefault("classEnumId", null)
        BigDecimal salvageValue = (BigDecimal) cs.getOrDefault("salvageValue", null)
        BigDecimal acquireCost = (BigDecimal) cs.getOrDefault("acquireCost", null)
        BigDecimal hoaFeeMonthly = (BigDecimal) cs.getOrDefault("hoaFeeMonthly", null)
        BigDecimal propertyTaxesMonthly = (BigDecimal) cs.getOrDefault("propertyTaxesMonthly", null)
        BigDecimal propertyInsuranceCostsMonthly = (BigDecimal) cs.getOrDefault("propertyInsuranceCostsMonthly", null)

        // validate fields
        sf.sync().name("mkdecision.dashboard.PartyServices.validate#PropertyFields")
                .parameters(cs)
                .call()
        if (mf.hasError()) {
            return new HashMap<String, Object>()
        }

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
                .parameter("financialFlowTypeEnumId", "MkFinFlowMonthlyPropertyTaxes")
                .parameter("assetId", assetId)
                .parameter("amount", propertyTaxesMonthly)
                .call()

        // create annual insurance costs
        sf.sync().name("create#mk.close.FinancialFlow")
                .parameter("partyId", partyId)
                .parameter("entryTypeEnumId", "MkEntryExpense")
                .parameter("financialFlowTypeEnumId", "MkFinFlowMonthlyInsuranceCosts")
                .parameter("assetId", assetId)
                .parameter("amount", propertyInsuranceCostsMonthly)
                .call()

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("partyId", partyId)
        outParams.put("assetId", assetId)
        return outParams
    }

    static void validateMortgageFields(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String lenderName = (String) cs.getOrDefault("lenderName", null)
        BigDecimal mortgageBalance = (BigDecimal) cs.getOrDefault("mortgageBalance", null)
        BigDecimal mortgagePaymentMonthly = (BigDecimal) cs.getOrDefault("mortgagePaymentMonthly", null)

        // validate lender name
        if (StringUtils.isBlank(lenderName)) {
            mf.addError(lf.localize("DASHBOARD_INVALID_LENDER_NAME"))
            return
        }

        // validate mortgage balance
        if (mortgageBalance != null && mortgageBalance <= 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_MORTGAGE_BALANCE"))
            return
        }

        // validate mortgage payment monthly
        if (mortgagePaymentMonthly != null && mortgagePaymentMonthly <= 0) {
            mf.addError(lf.localize("DASHBOARD_INVALID_MORTGAGE_PAYMENT_MONTHLY"))
        }
    }

    static Map<String, Object> createMortgage(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        ServiceFacade sf = ec.getService()
        UserFacade uf = ec.getUser()
        MessageFacade mf = ec.getMessage()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)
        String lenderName = (String) cs.getOrDefault("lenderName", null)
        BigDecimal mortgageBalance = (BigDecimal) cs.getOrDefault("mortgageBalance", null)
        BigDecimal mortgagePaymentMonthly = (BigDecimal) cs.getOrDefault("mortgagePaymentMonthly", null)

        // validate fields
        sf.sync().name("mkdecision.dashboard.PartyServices.validate#MortgageFields")
                .parameters(cs)
                .call()
        if (mf.hasError()) {
            return new HashMap<String, Object>()
        }

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
        String partyRelationshipId = lenderRelationshipResp.get("partyRelationshipId")

        // create mortgage
        sf.sync().name("create#mk.close.FinancialFlow")
                .parameter("partyId", partyId)
                .parameter("entryTypeEnumId", "MkEntryExpense")
                .parameter("financialFlowTypeEnumId", "MkFinFlowMortgage")
                .parameter("partyRelationshipId", partyRelationshipId)
                .parameter("balance", mortgageBalance)
                .parameter("amount", mortgagePaymentMonthly)
                .call()


        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("partyId", partyId)
        outParams.put("partyRelationshipId", partyRelationshipId)
        return outParams
    }

    static Map<String, Object> updateMortgage(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        ServiceFacade sf = ec.getService()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String partyId = (String) cs.getOrDefault("partyId", null)
        String partyRelationshipId = (String) cs.getOrDefault("partyRelationshipId", null)
        String lenderName = (String) cs.getOrDefault("lenderName", null)
        BigDecimal mortgageBalance = (BigDecimal) cs.getOrDefault("mortgageBalance", null)
        BigDecimal mortgagePaymentMonthly = (BigDecimal) cs.getOrDefault("mortgagePaymentMonthly", null)

        // validate fields
        sf.sync().name("mkdecision.dashboard.PartyServices.validate#MortgageFields")
                .parameters(cs)
                .call()
        if (mf.hasError()) {
            return new HashMap<String, Object>()
        }

        // validate relationship
        EntityValue relationship = ef.find("mantle.party.PartyRelationship")
                .condition("partyRelationshipId", partyRelationshipId)
                .one()
        if (relationship == null || !StringUtils.equals(partyId, relationship.getString("fromPartyId")) || !StringUtils.equals(relationship.getString("relationshipTypeEnumId"), "PrtMortgage")) {
            mf.addError(lf.localize("DASHBOARD_INVALID_MORTGAGE"))
            return new HashMap<String, Object>()
        }

        // update lender
        String lenderPartyId = (String) relationship.get("toPartyId")
        sf.sync().name("update#mantle.party.Organization")
                .parameter("partyId", lenderPartyId)
                .parameter("organizationName", lenderName)
                .call()

        // update mortgage
        EntityValue mortgageFinFlow = ef.find("mk.close.FinancialFlow")
                .condition("partyId", partyId)
                .condition("partyRelationshipId", partyRelationshipId)
                .condition("entryTypeEnumId", "MkEntryExpense")
                .condition("financialFlowTypeEnumId", "MkFinFlowMortgage")
                .list()
                .getFirst()
        sf.sync().name("update#mk.close.FinancialFlow")
                .parameter("financialFlowId", mortgageFinFlow.getString("financialFlowId"))
                .parameter("balance", mortgageBalance)
                .parameter("amount", mortgagePaymentMonthly)
                .call()

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("partyId", partyId)
        outParams.put("partyRelationshipId", partyRelationshipId)
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

        // delete mortgage
        EntityValue mortgageFinancialFlow = ef.find("mk.close.FinancialFlow")
                .condition("partyId", partyId)
                .condition("entryTypeEnumId", "MkEntryExpense")
                .condition("financialFlowTypeEnumId", "MkFinFlowMortgage")
                .condition("partyRelationshipId", partyRelationshipId)
                .list()
                .getFirst()
        sf.sync().name("delete#mk.close.FinancialFlow")
                .parameter("financialFlowId", mortgageFinancialFlow.getString("financialFlowId"))
                .call()

        // delete relationship
        sf.sync().name("delete#mantle.party.PartyRelationship")
                .parameter("partyRelationshipId", partyRelationshipId)
                .call()

        // return the output parameters
        return new HashMap<>()
    }
}
