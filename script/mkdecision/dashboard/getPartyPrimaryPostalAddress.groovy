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

// get the postal addresses
EntityList postalAddresses = ef.find("mantle.party.contact.PartyContactMechPostalAddress")
        .condition("partyId", partyId)
        .condition("contactMechPurposeId", "PostalPrimary")
        .conditionDate("fromDate", "thruDate", uf.getNowTimestamp())
        .list();

// prepare address
String postalAddress = null;
if (postalAddresses != null && !postalAddresses.isEmpty()) {
    EntityValue firstPostalAddress = postalAddresses.getFirst();
    postalAddress = String.format("%s, %s %s", firstPostalAddress.getString("address1"), firstPostalAddress.getString("stateGeoCodeAlpha2"), firstPostalAddress.getString("postalCode"));
}

// return the output parameters
HashMap<String, Object> outParams = new HashMap<>()
outParams.put("postalAddress", postalAddress)
return outParams