package com.spendsense.api.service.ai;

interface AiProvider {
    AiProviderResult generate(AiProviderRequest request);
}
