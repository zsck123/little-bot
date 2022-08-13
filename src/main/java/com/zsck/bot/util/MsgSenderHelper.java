package com.zsck.bot.util;

import com.zsck.bot.enums.MsgType;
import lombok.AllArgsConstructor;
import lombok.Data;
import love.forte.simbot.api.message.events.GroupMsg;
import love.forte.simbot.api.message.events.MessageGet;
import love.forte.simbot.api.message.events.MsgGet;
import love.forte.simbot.api.sender.MsgSender;
import love.forte.simbot.component.mirai.message.MiraiGroupMsgFlag;

import javax.xml.ws.soap.Addressing;

/**
 * @author QQ:825352674
 * @date 2022/8/13 - 12:57
 */
@AllArgsConstructor
public class MsgSenderHelper{
    private MsgType msgType;
    private String number;
    private MsgSender sender;
    public void senderMsg(String msg){
        if (msgType == MsgType.GROUP){
            sender.SENDER.sendGroupMsg(number, msg);
        }else if (msgType == MsgType.PRIVATE){
            sender.SENDER.sendPrivateMsg(number, msg);
        }
    }

    public String getNumber() {
        return number;
    }

    private MsgSenderHelper(MsgType msgType, MsgSender sender) {
        this.msgType = msgType;
        this.sender = sender;
    }

    private void setNumber(String number) {
        this.number = number;
    }

    public static MsgSenderHelper getInstance(MsgGet msgGet, MsgSender sender){
        MsgType msgType = getSenderType(msgGet);
        MsgSenderHelper msgSenderHelper = new MsgSenderHelper(msgType, sender);
        if (msgType == MsgType.GROUP){
            msgSenderHelper.setNumber(((GroupMsg) msgGet).getGroupInfo().getGroupCode());
        }else{
            msgSenderHelper.setNumber(msgGet.getAccountInfo().getAccountCode());
        }
        return msgSenderHelper;
    }

    private static MsgType getSenderType(MsgGet msgGet){
        if (msgGet.getOriginalData().startsWith("Group")){
            return MsgType.GROUP;
        }
        return MsgType.PRIVATE;
    }
}