package com.mkdecision.dashboard

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.time.DateUtils
import org.moqui.context.ExecutionContext
import org.moqui.context.L10nFacade
import org.moqui.context.MessageFacade
import org.moqui.context.ResourceFacade
import org.moqui.context.UserFacade
import org.moqui.entity.*
import org.moqui.impl.entity.EntityFacadeImpl
import org.moqui.service.ServiceFacade
import org.moqui.util.ContextStack

class AgreementServices {

    static Map<String, Object> countOrderAgreementSignatures(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        UserFacade uf = ec.getUser()

        // get the parameters
        String orderId = (String) cs.getOrDefault("orderId", null)

        // find order agreements
        EntityList orderAgreements = ef.find("mantle.order.OrderAgreement")
                .condition("orderId", orderId)
                .list()

        // find order parties
        EntityList orderParties = ef.find("mantle.order.OrderPartParty")
                .condition("orderId", orderId)
                .condition("orderPartSeqId", "01")
                .list()

        // count agreement signatures
        long customersSigned = 0
        long customersPending = 0
        long dealersSigned = 0
        long dealersPending = 0
        long userSigned = 0
        long userPending = 0
        String userPartyId = uf.userAccount.getString("partyId")
        for (EntityValue orderAgreement : orderAgreements) {
            String agreementId = orderAgreement.getString("agreementId")
            for (EntityValue orderParty : orderParties) {
                String partyId = orderParty.getString("partyId")
                String roleTypeId = orderParty.getString("roleTypeId")
                long signatureCount = ef.find("mantle.party.agreement.AgreementSignature")
                        .condition("agreementId", agreementId)
                        .condition("partyId", partyId)
                        .count()
                if (signatureCount > 0) {
                    if (roleTypeId == "PrimaryApplicant" || roleTypeId == "CoApplicant") {
                        customersSigned++
                    } else {
                        dealersSigned++
                    }
                    if (partyId == userPartyId) {
                        userSigned++
                    }
                } else {
                    if (roleTypeId == "PrimaryApplicant" || roleTypeId == "CoApplicant") {
                        customersPending++
                    } else {
                        dealersPending++
                    }
                    if (partyId == userPartyId) {
                        userPending++
                    }
                }
            }
        }

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("orderId", orderId)
        outParams.put("customersSigned", customersSigned)
        outParams.put("customersPending", customersPending)
        outParams.put("dealersSigned", dealersSigned)
        outParams.put("dealersPending", dealersPending)
        outParams.put("userSigned", userSigned)
        outParams.put("userPending", userPending)
        return outParams
    }

    static Map<String, Object> signOrderAgreement(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()
        ServiceFacade sf = ec.getService()
        UserFacade uf = ec.getUser()
        MessageFacade mf = ec.getMessage()
        L10nFacade lf = ec.getL10n()

        // get the parameters
        String orderId = (String) cs.getOrDefault("orderId", null)
        String agreementTypeEnumId = (String) cs.getOrDefault("agreementTypeEnumId", null)
        String templateLocation = (String) cs.getOrDefault("templateLocation", null)
        String agreementServiceGenerator = (String) cs.getOrDefault("serviceName", null)
        String partyId = uf.userAccount.getString("partyId")
        String textData = getAgreementText(sf, orderId, templateLocation, agreementServiceGenerator)

        // validate order
        EntityValue orderHeader = ef.find("mantle.order.OrderHeader")
                .condition("orderId", orderId)
                .one()
        if (orderHeader == null) {
            mf.addError(lf.localize("DASHBOARD_INVALID_ORDER"))
            return new HashMap<String, Object>()
        }

        // find order part
        EntityValue orderPart = ef.find("mantle.order.OrderPart")
                .condition("orderId", orderId)
                .condition("orderPartSeqId", "01")
                .one()

        // find product store
        EntityValue productStore = ef.find("mantle.product.store.ProductStore")
                .condition("productStoreId", orderHeader.getString("productStoreId"))
                .one()

        // create agreement
        Map<String, Object> agreementResp = sf.sync().name("create#mantle.party.agreement.Agreement")
                .parameter("agreementTypeEnumId", agreementTypeEnumId)
                .parameter("statusId", "MkAgreeDraft")
                .parameter("organizationPartyId", productStore.getString("organizationPartyId"))
                .parameter("organizationRoleTypeId", "Vendor")
                .parameter("agreementDate", uf.nowTimestamp)
                .parameter("fromDate", uf.nowTimestamp)
                .parameter("textData", textData)
                .call()
        String agreementId = (String) agreementResp.get("agreementId")

        sf.sync().name("create#mantle.order.OrderAgreement")
                .parameter("orderId", orderId)
                .parameter("orderPartSeqId", orderPart.getString("orderPartSeqId"))
                .parameter("agreementId", agreementId)
                .call()
        
        sf.sync().name("create#mantle.party.agreement.AgreementParty")
                .parameter("agreementId", agreementId)
                .parameter("partyId",orderPart.getString("customerPartyId") )
                .parameter("roleTypeId", "PrimaryApplicant")
                .call()

        sf.sync().name("close.AgreementServices.sign#AgreementWithRole")
                .parameter("agreementId", agreementId)
                .parameter("partyId", partyId)
                .parameter("roleTypeId", "FinanceManager")
                .call()

        // return the output parameters
        Map<String, Object> outParams = new HashMap<>()
        outParams.put("orderId", orderId)
        outParams.put("partyId", partyId)
        outParams.put("agreementId", agreementId)
        return outParams
    }

    static String getAgreementText(ServiceFacade sf, String orderId, String templateLocation, String agreementServiceGenerator = null) {

        // generate template parameters
        Map<String, Object> templateParameters = [:]
        if (agreementServiceGenerator) {
            templateParameters = sf.sync().name(agreementServiceGenerator)
                    .parameter("orderId", orderId)
                    .call()
        }

        // create agreement text
        Map<String, Object> agreement = sf.sync().name("close.AgreementServices.create#AgreementText")
                .parameter("templateLocation", templateLocation)
                .parameter("templateParameters", templateParameters)
                .call()

        // return agreement text
        return agreement.textData
    }
}
