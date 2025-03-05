package com.tencent.supersonic.chat.server.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.tencent.supersonic.chat.api.pojo.request.TextVoiceReq;
import com.tencent.supersonic.chat.server.service.VoiceService;

@Controller
@RequestMapping("/api/chat/voice")
public class VoiceController {
    
    @Autowired
    private VoiceService voiceService;

    @PostMapping("/iat")
    @ResponseBody
    public String voiceText(@RequestBody byte[] voice) {
        return voiceService.voiceText(voice);
    }
    
    @PostMapping("/tts")
    public ResponseEntity<byte[]> textVoice(@RequestBody TextVoiceReq textVoiceReq) {
        byte[] voice = voiceService.textVoice(textVoiceReq);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(voice);
    }
    
}
