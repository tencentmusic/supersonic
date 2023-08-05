package com.tencent.supersonic.chat.utils;


import com.plexpt.chatgpt.ChatGPT;
import com.plexpt.chatgpt.entity.chat.ChatCompletion;
import com.plexpt.chatgpt.entity.chat.ChatCompletionResponse;
import com.plexpt.chatgpt.entity.chat.Message;
import com.plexpt.chatgpt.util.Proxys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.Proxy;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;


@Component
public class ChatGptHelper {

    @Value("${llm.chatgpt.apikey:sk-kdgPxxx}")
    private String apiKey;

    @Value("${llm.chatgpt.apiHost:https://api.openai.com/}")
    private String apiHost;

    @Value("${llm.chatgpt.proxyIp:default}")
    private String proxyIp;

    @Value("${llm.chatgpt.proxyPort:8080}")
    private Integer proxyPort;


    public ChatGPT getChatGPT(){
        Proxy proxy = null;
        if (!"default".equals(proxyIp)){
            proxy = Proxys.http(proxyIp, proxyPort);
        }
        return ChatGPT.builder()
                .apiKey(apiKey)
                .proxy(proxy)
                .timeout(900)
                .apiHost(apiHost) //反向代理地址
                .build()
                .init();
    }

    public String inferredTime(String queryText){
        long nowTime = System.currentTimeMillis();
        Date date = new Date(nowTime);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = sdf.format(date);
        Message system = Message.ofSystem("现在时间 "+formattedDate+"，你是一个专业的数据分析师，你的任务是基于数据，专业的解答用户的问题。" +
                "你需要遵守以下规则：\n" +
                "1.返回规范的数据格式，json，如： 输入：近 10 天的日活跃数，输出：{\"start\":\"2023-07-21\",\"end\":\"2023-07-31\"}" +
                "2.你对时间数据要求规范，能从近 10 天，国庆节，端午节，获取到相应的时间，填写到 json 中。\n"+
                "3.你的数据时间，只有当前及之前时间即可,超过则回复去年\n" +
                "4.只需要解析出时间，时间可以是时间月和年或日、日历采用公历\n"+
                "5.时间给出要是绝对正确，不能瞎编\n"
        );
        Message message = Message.of("输入："+queryText+"，输出：");
        ChatCompletion chatCompletion = ChatCompletion.builder()
                .model(ChatCompletion.Model.GPT_3_5_TURBO_16K.getName())
                .messages(Arrays.asList(system, message))
                .maxTokens(10000)
                .temperature(0.9)
                .build();
        ChatCompletionResponse response = getChatGPT().chatCompletion(chatCompletion);
        Message res = response.getChoices().get(0).getMessage();
        return res.getContent();
    }

    public static void main(String[] args) {

    }


}
