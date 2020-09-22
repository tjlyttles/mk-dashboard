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
EntityList emails = ef.find("mantle.party.contact.PartyContactMechInfo")
        .condition("partyId", partyId)
        .condition("contactMechPurposeId", "EmailPrimary")
        .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
        .list();

// prepare email address
String emailAddress = null;
if (emails != null && !emails.isEmpty()) {
    EntityValue firstEmail = emails.getFirst();
    emailAddress = firstEmail.getString("infoString");
}

// return the output parameters
HashMap<String, Object> outParams = new HashMap<>()
outParams.put("emailAddress", emailAddress)
return outParams