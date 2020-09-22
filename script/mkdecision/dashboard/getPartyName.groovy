import org.moqui.context.*
import org.moqui.entity.*;
import org.moqui.util.*
import org.apache.commons.lang3.StringUtils;

// shortcuts for convenience
ExecutionContext ec = context.ec
ContextStack cs = ec.getContext()
EntityFacade ef = ec.getEntity()

// get the parameters
String partyId = (String) cs.getOrDefault("partyId", null)

// get the party
EntityValue partyDetail = ef.find("mantle.party.PartyDetail")
        .condition("partyId", partyId)
        .one();

// prepare party name
String partyName = null;
if (partyDetail != null) {
    if (partyDetail.getString("partyTypeEnumId").equals("PtyPerson")) {
        partyName = String.format("%s %s", StringUtils.defaultString(partyDetail.getString("firstName")), StringUtils.defaultString(partyDetail.getString("lastName"))).trim();
    } else if (partyDetail.getString("partyTypeEnumId").equals("PtyOrganization")) {
        partyName = partyDetail.getString("organizationName")
    }
}

// return the output parameters
HashMap<String, Object> outParams = new HashMap<>()
outParams.put("partyName", partyName)
return outParams