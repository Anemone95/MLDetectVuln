package top.anemone.tainttarget;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class XssController {
    @RequestMapping("/simpleXSS3")
    public String xss3(String xss){
        return xss.replace("&","&amp;")
                .replace("<","&lt;")
                .replace(">", "&gt;")
                .replace(" ", "&nbsp;")
                .replace("\"", "&#34;")
                .replace("'", "&#39;");
    }
}
