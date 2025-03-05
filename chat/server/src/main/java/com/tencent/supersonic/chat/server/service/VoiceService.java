package com.tencent.supersonic.chat.server.service;

import com.tencent.supersonic.chat.api.pojo.request.TextVoiceReq;

public interface VoiceService {

    String voiceText(byte[] data);

    byte[] textVoice(TextVoiceReq textVoiceReq);

}
