package com.kindlepocket.web.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.kindlepocket.web.service.SubscriberService;
import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.kindlepocket.web.service.TextBookInfoSearchService;
import com.kindlepocket.web.util.CheckUtil;
import com.kindlepocket.web.util.MessageUtil;

@Controller
@RequestMapping("/Weixin")
public class KindlePocketController {

    private static final long serialVersionUID = 1L;

    private static String SUBSCRIBER_OPENID = null;

    @Autowired
    private TextBookInfoSearchService searchService;// = new TextBookInfoSearchService();

    @Autowired
    private SubscriberService ssbService;

    private static Logger logger = Logger.getLogger(KindlePocketController.class);

    @RequestMapping("/homepage")
    public String toIndex() {
        if (logger.isInfoEnabled()) {
            logger.info("redirecting to homePage...");
        }
        return "index";
    }

    @RequestMapping("/details")
    public String toSearchDetailPage() {
        return "details";
    }

    @RequestMapping("/toBindingPage")
    public String toBindingPage(HttpServletRequest request, HttpServletResponse response, @RequestParam("subscriberOpenId") String subscriberOpenId, Model model) {
        if (logger.isInfoEnabled()) {
            logger.info("redirecting to binding page; openId:" + subscriberOpenId);
        }

        //model.addAttribute("subscriberOpenId",subscriberOpenId);
        Cookie cookie = new Cookie("subscriberOpenId", subscriberOpenId);
        cookie.setPath("/");
        cookie.setMaxAge(Integer.MAX_VALUE);
        response.addCookie(cookie);

        return "binding";
    }

    @RequestMapping(value = "/bindingData", method = RequestMethod.POST)
    public String bindingData(HttpServletRequest request, HttpServletResponse response, @RequestParam("phone") String phone, @RequestParam("email") String email, @RequestParam("emailPwd") String emailPwd, @RequestParam("kindleEmail") String kindleEmail) {
        if (logger.isInfoEnabled()) {
            logger.info("phone:" + phone + " email:" + email + " emailPwd:" + emailPwd + " kindleEmail:" + kindleEmail);
        }

        Cookie[] cookies = request.getCookies();
        if (null != cookies) {
            for (Cookie cookie : cookies) {

                System.out.println("cookieName:" + cookie.getName() + " cookieValue:" + cookie.getValue());

                String subscriberOpenIdKey = cookie.getName();
                String subscriberOpenId = cookie.getValue();
                if (subscriberOpenIdKey.equalsIgnoreCase("subscriberOpenId")) {
                    this.ssbService.binding(subscriberOpenId, phone, email, emailPwd, kindleEmail);
                    if (logger.isInfoEnabled()) {
                        logger.info("subscriber: " + subscriberOpenId + "has binded information!");
                    }
                } else {
                    if (logger.isInfoEnabled()) {
                        logger.info("no valid subscriber information received!");
                    }
                }
            }
        } else {
            System.out.println("no cookie received");
        }

        return "binding";
    }

    @RequestMapping(value = "/wx.do", method = RequestMethod.GET)
    public void validate(HttpServletRequest request, HttpServletResponse response,
                         @RequestParam("signature") String signature, @RequestParam("timestamp") String timestamp,
                         @RequestParam("nonce") String nonce, @RequestParam("echostr") String echostr) {
        //http://33051bbe.nat123.net/Weixin/wx.do
        if (logger.isInfoEnabled()) {
            logger.info("\n***The message got: signature:" + signature + " timestamp:" + timestamp
                    + " nonce:" + nonce + " echostr:" + echostr);
        }

        PrintWriter out = null;
        try {
            out = response.getWriter();
        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("response error");
            }
        }

        if (CheckUtil.checkSignature(signature, timestamp, nonce)) {
            out.print(echostr);
            if (logger.isInfoEnabled()) {
                logger.info("validated!");
            }
        }
    }

    @RequestMapping(value = "/wx.do", method = RequestMethod.POST)
    public void processMessage(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = null;
        try {
            out = response.getWriter();
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {

            Map<String, String> map = MessageUtil.xmlToMap(request);
            String fromUserName = map.get("FromUserName");
            String toUserName = map.get("ToUserName");
            String msgType = map.get("MsgType");
            String content = map.get("Content");

            this.SUBSCRIBER_OPENID = fromUserName;

            if (logger.isInfoEnabled()) {
                logger.info("\n***The message got: fromUserName:" + fromUserName + " toUserName:"
                        + toUserName + " msgType:" + msgType + " content:" + content);
            }

            String responseMessage = null;
            if (MessageUtil.MESSAGE_TEXT.equals(msgType)) {

                switch (content) {
                    case "1":
                        responseMessage = MessageUtil.initSinglePicTextMessage(toUserName, fromUserName, "kindlePocket", "kindle text books sharing platform", "/imgs/welcome.jpg", "/Weixin/binding");
                        break;
                    case "2":
                        responseMessage = MessageUtil.initSinglePicTextMessage(toUserName, fromUserName, "绑定步骤", "点击绑定kindle", "/imgs/welcome.jpg", "/Weixin/toBindingPage?subscriberOpenId=" + fromUserName);
                        break;
                    case "menu":
                        responseMessage = MessageUtil.initText(toUserName, fromUserName, MessageUtil.menuText());

                        break;
                    default:
                        // responseMessage = MessageUtil.initText(toUserName, fromUserName, MessageUtil.menuText());
                        List<String> titleList = this.searchService.search(content);
                        logger.info("找到" + titleList.size() + "本书籍");
                        responseMessage = MessageUtil.initSearchResultsPicTextMessage(toUserName, fromUserName, titleList);
                        break;
                }

                /*
                 * TextMessage textMessage = new TextMessage(); textMessage.setFromUserName(toUserName); textMessage.setToUserName(fromUserName); textMessage.setMsgType("text"); textMessage.setCreateTime(new Date().getTime()); textMessage.setContent("the message you sent was : " + content); responseMessage = MessageUtil.textMessageToXml(textMessage); if (logger.isInfoEnabled()) { logger.info("the message responsed is :\n" + responseMessage); }
                 */
            } else if (MessageUtil.MESSAGE_EVENT.equals(msgType)) {
                String eventType = map.get("Event");
                if (MessageUtil.MESSAGE_SUBSCRIBE.equals(eventType)) {

                    responseMessage = MessageUtil.initText(toUserName, fromUserName,
                            MessageUtil.welcomeText());

                }
            }
            if (logger.isInfoEnabled()) {
                logger.info("\n***The message responsed: \n" + responseMessage);
            }
            out.print(responseMessage);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

  /*  @RequestMapping("/test")
    public void test(HttpServletResponse response,HttpServletRequest request){
        Cookie cookie = new Cookie("subscriberOpenId","123");
        cookie.setPath("/");
        cookie.setMaxAge(Integer.MAX_VALUE);
        response.addCookie(cookie);
        PrintWriter out = null;
        try {
            out = response.getWriter();
        } catch (IOException e) {
            e.printStackTrace();
        }
        out.print("123");
    }*/

    /*
     * public static void main(String[] args) { SpringApplication.run(KindlePocketController.class, args); }
     */

}
