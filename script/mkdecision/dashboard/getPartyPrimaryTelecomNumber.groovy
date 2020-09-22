import org.moqui.context.*
import org.moqui.entity.*;
import org.moqui.util.*
import org.apache.commons.lang3.StringUtils;

// shortcuts for convenience
ExecutionContext ec = context.ec
ContextStack cs = ec.getContext()
EntityFacade ef = ec.getEntity()
UserFacade uf = ec.getUser()

// get the parameters
String partyId = (String) cs.getOrDefault("partyId", null)

// get the telecom numbers
EntityList telecomNumbers = ef.find("mantle.party.contact.PartyContactMechTelecomNumber")
        .condition("partyId", partyId)
        .condition("contactMechPurposeId", "PhonePrimary")
        .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
        .list();

// prepare telecom number
String telecomNumber = null;
if (telecomNumbers != null && !telecomNumbers.isEmpty()) {
    EntityValue firstNumber = telecomNumbers.getFirst();
    telecomNumber = firstNumber.getString("contactNumber");
}

// return the output parameters
HashMap<String, Object> outParams = new HashMap<>()
outParams.put("telecomNumber", telecomNumber)
return outParams