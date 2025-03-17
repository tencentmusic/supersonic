package com.tencent.supersonic.chat.server.rest;


import com.tencent.supersonic.chat.api.pojo.request.TextVoiceReq;
import com.tencent.supersonic.chat.server.service.VoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/voice")
public class VoiceController {

    @Autowired
    private VoiceService voiceService;

    @PostMapping("/iat")
    public String voiceText(@RequestBody byte[] voice) {
        return voiceService.voiceText(voice);
    }

    @PostMapping("/tts")
    public String textVoice(@RequestBody TextVoiceReq textVoiceReq) {
        return voiceService.textVoice(textVoiceReq);
    }

}
