package Utils;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ai.chat.Message;
import burp.api.montoya.ai.chat.PromptOptions;
import burp.api.montoya.ai.chat.PromptResponse;


public class AI {

    private String prompt;
    private static MontoyaApi api;

    public AI(MontoyaApi montoyaapi){
        api = montoyaapi;
    }


    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public static boolean isAiSupported() {
        return  api != null && hasApiMethod(api, "ai") && api.ai().isEnabled();
    }


    public static boolean hasApiMethod(Object obj, String methodName) {
        try {
            Class<?> clazz = obj.getClass();
            clazz.getMethod(methodName);
            return true;
        } catch(NoSuchMethodException e){
            return false;
        }
    }


    public String execute() {
        if(!isAiSupported()) {
            throw new RuntimeException("Montoya AI API is not enabled. You need to enable use AI in the extension tab.");
        }
        PromptResponse response = api.ai().prompt().execute(PromptOptions.promptOptions().withTemperature(1.0), Message.userMessage(this.prompt));
        return response.content();
    }

}
