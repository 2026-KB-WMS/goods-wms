package com.kb.wmslab.goods_wms.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

class RecordSupportingTypeResolver extends ObjectMapper.DefaultTypeResolverBuilder {

    RecordSupportingTypeResolver(ObjectMapper.DefaultTyping typing, PolymorphicTypeValidator ptv) {
        super(typing, ptv);
    }

    @Override
    public boolean useForType(JavaType type) {
        if (type.getRawClass().isRecord()) return true;
        return super.useForType(type);
    }
}
