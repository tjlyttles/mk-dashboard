package com.mkdecision.dashboard

import org.apache.commons.lang3.StringUtils
import org.moqui.context.ExecutionContext
import org.moqui.context.UserFacade
import org.moqui.entity.EntityFacade
import org.moqui.entity.EntityList
import org.moqui.entity.EntityValue
import org.moqui.util.ContextStack;

class ProductServices {

    static Map<String, Object> getProductParameterValue(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()

        // get the parameters
        String productId = (String) cs.getOrDefault("productId", null)
        String productStoreId = (String) cs.getOrDefault("productStoreId", null)
        String productParameterId = (String) cs.getOrDefault("productParameterId", null)

        // get the loan fee parameters
        EntityList parameterList = ef.find("mantle.product.ProductParameterOption")
                .condition("productId", productId)
                .condition("productStoreId", productStoreId)
                .condition("productParameterId", productParameterId)
                .list();
        EntityValue parameter = parameterList.isEmpty() ? null : parameterList.getFirst();

        // return the output parameters
        HashMap<String, Object> outParams = new HashMap<>()
        outParams.put("productId", productId)
        outParams.put("productParameterId", productParameterId)
        outParams.put("parameterValue", parameter == null ? null : parameter.getString("parameterValue"))
        return outParams
    }

    static Map<String, Object> getProductPrice(ExecutionContext ec) {

        // shortcuts for convenience
        ContextStack cs = ec.getContext()
        EntityFacade ef = ec.getEntity()

        // get the parameters
        String productId = (String) cs.getOrDefault("productId", null)
        String productStoreId = (String) cs.getOrDefault("productStoreId", null)
        String priceTypeEnumId = (String) cs.getOrDefault("priceTypeEnumId", null)

        // get the product prices
        EntityList priceList = ef.find("mantle.product.ProductPrice")
                .condition("productId", productId)
                .condition("productStoreId", productStoreId)
                .condition("priceTypeEnumId", priceTypeEnumId)
                .conditionDate("fromDate", "thruDate", ec.user.getNowTimestamp())
                .list();
        EntityValue price = priceList.isEmpty() ? null : priceList.getFirst();

        // return the output parameters
        HashMap<String, Object> outParams = new HashMap<>()
        outParams.put("productId", productId)
        outParams.put("productStoreId", productStoreId)
        outParams.put("priceTypeEnumId", priceTypeEnumId)
        outParams.put("price", price == null ? null : price.getBigDecimal("price"))
        outParams.put("priceUomId", price == null ? null : price.getString("priceUomId"))
        return outParams
    }
}
