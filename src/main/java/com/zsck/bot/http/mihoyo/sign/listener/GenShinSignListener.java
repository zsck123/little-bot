package com.zsck.bot.http.mihoyo.sign.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zsck.bot.helper.MsgSenderHelper;
import com.zsck.bot.http.mihoyo.sign.GenShinSign;
import com.zsck.bot.http.mihoyo.sign.exception.GenShinNoSuchUIDException;
import com.zsck.bot.http.mihoyo.sign.pojo.GenshinInfo;
import com.zsck.bot.http.mihoyo.sign.service.GenshinService;
import com.zsck.bot.util.ContextUtil;
import kotlinx.coroutines.TimeoutCancellationException;
import lombok.extern.slf4j.Slf4j;
import love.forte.simbot.annotation.Filter;
import love.forte.simbot.annotation.OnGroup;
import love.forte.simbot.annotation.OnPrivate;
import love.forte.simbot.annotation.OnlySession;
import love.forte.simbot.api.message.MessageContentBuilder;
import love.forte.simbot.api.message.MessageContentBuilderFactory;
import love.forte.simbot.api.message.events.GroupMsg;
import love.forte.simbot.api.message.events.PrivateMsg;
import love.forte.simbot.api.sender.MsgSender;
import love.forte.simbot.component.mirai.message.MiraiMessageContentBuilder;
import love.forte.simbot.component.mirai.message.MiraiMessageContentBuilderFactory;
import love.forte.simbot.filter.MatchType;
import love.forte.simbot.listener.ContinuousSessionScopeContext;
import love.forte.simbot.listener.ListenerContext;
import love.forte.simbot.listener.SessionCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author QQ:825352674
 * @date 2022/9/2 - 19:12
 */
@Slf4j
@DependsOn("contextUtil")
@Component
public class GenShinSignListener {
    @Autowired
    private GenShinSign genShinSign;
    @Autowired
    private GenshinService genshinService;
    @Autowired
    private MessageContentBuilderFactory factory;
    private final MiraiMessageContentBuilderFactory miraiFactory = ContextUtil.getForwardBuilderFactory();
    private final static String KEY_START = "==KEY_START==";
    private final static String GENSHIN_SIGN = "GENSHIN_SIGN:COOKIE";
    private final static String GENSHIN_SIGN_CHOOSE_UID = "GENSHIN_SIGN:COOKIE_CHOOSE_UID";

    @Filter(value = "ys??????", matchType = MatchType.EQUALS)
    @OnGroup
    public void signReady(GroupMsg groupMsg, MsgSender sender){
        MsgSenderHelper senderHelper = MsgSenderHelper.getInstance(groupMsg, sender);
        String qqNumber = groupMsg.getAccountInfo().getAccountCode();


        LambdaQueryWrapper<GenshinInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GenshinInfo::getQqNumber, qqNumber);
        List<GenshinInfo> list = genshinService.list(wrapper);
        if (list.isEmpty()){
            senderHelper.GROUP.sendMsg("[CAT:at,code=" + qqNumber + "]???????????????????????????????????????");
        } else {
            MiraiMessageContentBuilder forwardBuilder = miraiFactory.getMessageContentBuilder();
            forwardBuilder.forwardMessage( fun ->{
                list.forEach( info -> fun.add(groupMsg.getBotInfo(), genShinSign.doSign(info)));
            });
            senderHelper.GROUP.sendMsg(forwardBuilder.build());
        }
    }

    @Filter(value = "??????????????????", matchType = MatchType.EQUALS)
    @OnGroup
    public void unbindGenshin(GroupMsg groupMsg, MsgSender sender, ListenerContext context) {
        MsgSenderHelper senderHelper = MsgSenderHelper.getInstance(groupMsg, sender);

        MessageContentBuilder builder = factory.getMessageContentBuilder();
        builder.at(senderHelper.getQqNumber());
        String key = KEY_START + ":" + senderHelper.getQqNumber();

        senderHelper.GROUP.sendMsg("?????????????????????????????????");


        LambdaQueryWrapper<GenshinInfo> listWrapper = new LambdaQueryWrapper<>();
        listWrapper.eq(GenshinInfo::getQqNumber, senderHelper.getQqNumber());
        List<GenshinInfo> list = genshinService.list(listWrapper);
        if (!list.isEmpty()) {
            StringBuffer infoDetail = new StringBuffer("??????????????????????????????:\n");
            list.forEach(info -> infoDetail.append(info.getUid()).append(" : ").append(info.getNickName()).append("\n"));
            infoDetail.append("??????????????????????????????uid");
            senderHelper.PRIVATE.sendMsg(infoDetail.toString());

            ContinuousSessionScopeContext scopeContext = (ContinuousSessionScopeContext) context.getContext(ListenerContext.Scope.CONTINUOUS_SESSION);

            context.getContext(ListenerContext.Scope.GLOBAL);
            SessionCallback<String> callback = SessionCallback.builder(String.class).onResume(uid -> {
                AtomicReference<GenshinInfo> des = new AtomicReference<>();
                list.forEach(info -> {
                    if (info.getUid().equals(uid)) {
                        des.set(info);
                    }
                });

                LambdaQueryWrapper<GenshinInfo> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(GenshinInfo::getUid, uid);

                if (des.get() != null) {
                    genshinService.remove(wrapper);
                    StringBuffer buffer = new StringBuffer("????????????????????????:");
                    buffer.append(uid).append(" : ").append(des.get().getNickName());
                    senderHelper.PRIVATE.sendMsg(buffer.toString());
                } else {
                    senderHelper.PRIVATE.sendMsg("?????????uid");
                }
            }).onError(exception -> {
                if (exception instanceof TimeoutCancellationException){
                    senderHelper.PRIVATE.sendMsg("???????????????????????????????????????");
                }else {
                    senderHelper.PRIVATE.sendMsg("??????????????????????????????");
                }
            }).build();
            scopeContext.waiting(GENSHIN_SIGN_CHOOSE_UID, key, 120000, callback);
        }else {
            senderHelper.GROUP.sendMsg("?????????????????????????????????");
        }
    }


    @Filter(value = "??????????????????", matchType = MatchType.EQUALS)
    @OnGroup
    public void bindGenshin(GroupMsg groupMsg, MsgSender sender, ListenerContext context) {
        MsgSenderHelper senderHelper = MsgSenderHelper.getInstance(groupMsg, sender);

        MessageContentBuilder builder = factory.getMessageContentBuilder();
        builder.at(senderHelper.getQqNumber());

        String key = KEY_START + ":" + senderHelper.getQqNumber();

        ContinuousSessionScopeContext scopeContext = (ContinuousSessionScopeContext) context.getContext(ListenerContext.Scope.CONTINUOUS_SESSION);
        SessionCallback<String> callback = SessionCallback.builder(String.class).onResume( cookie ->{
            List<GenshinInfo> infoList = genShinSign.analyzeCookie(cookie);
            AtomicReference<GenshinInfo> info = new AtomicReference<>(null);
            if (infoList.size() == 1){
                //?????????????????????????????????
                saveGenshinInfo(infoList.get(0));

                senderHelper.GROUP.sendMsg("[CAT:at,code=" + senderHelper.getQqNumber() + "]????????????,uid:" + infoList.get(0).getUid());
            }else {
                //?????????????????????
                senderHelper.PRIVATE.sendMsg("???????????????????????????????????????uid");
                StringBuffer buffer = new StringBuffer("?????????\n");
                infoList.forEach( res -> buffer.append(res.getUid()).append(":").append(res.getNickName()).append("\n"));
                senderHelper.PRIVATE.sendMsg(buffer.toString());

                //?????????????????????????????????uid
                SessionCallback<String> uidCallback = SessionCallback.builder(String.class).onResume(uid -> {
                    for (GenshinInfo genshinInfo : infoList) {
                        if (genshinInfo.getUid().equals(uid)) {
                            info.set(genshinInfo);
                        }
                    }
                    if (info.get() == null) {
                        throw new GenShinNoSuchUIDException("cookie?????????????????????uid?????????????????????????????????uid");
                    }

                    info.get().setQqNumber(senderHelper.getQqNumber());

                    saveGenshinInfo(info.get());

                    senderHelper.GROUP.sendMsg("[CAT:at,code=" + senderHelper.getQqNumber() + "]????????????,uid:" + info.get().getUid());

                }).onError( exception -> {
                    if (exception instanceof TimeoutCancellationException){
                        senderHelper.GROUP.sendMsg("[CAT:at,code=" + senderHelper.getQqNumber() + "]????????????????????????????????????????????????????????? ?????????????????? ????????????");
                        return;
                    }else if (exception instanceof GenShinNoSuchUIDException){
                        senderHelper.GROUP.sendMsg("[CAT:at,code=" + senderHelper.getQqNumber() + "]" + exception.getMessage());
                    }
                }).build();
                scopeContext.waiting(GENSHIN_SIGN_CHOOSE_UID, key, 120000, uidCallback);
            }

        }).onError(exception -> {
            if (exception instanceof TimeoutCancellationException){
                senderHelper.GROUP.sendMsg("[CAT:at,code=" + senderHelper.getQqNumber() + "]????????????????????????????????????????????????????????? ?????????????????? ????????????");
                return;
            }
            senderHelper.GROUP.sendMsg("[CAT:at,code=" + senderHelper.getQqNumber() + "]cookie??????");
        }).build();

        scopeContext.waiting(GENSHIN_SIGN, key,300000 , callback);

        senderHelper.GROUP.sendMsg("[CAT:at,code=" + senderHelper.getQqNumber() + "]????????????????????????cookie????????????\n (???:???????????????????????????????????????)");
    }

    private void saveGenshinInfo(GenshinInfo info){
        LambdaQueryWrapper<GenshinInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GenshinInfo::getUid, info.getUid());
        GenshinInfo genshinInfo = genshinService.getOne(wrapper);
        if (genshinInfo == null) {
            genshinService.save(info);
        }else {
            LambdaUpdateWrapper<GenshinInfo> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(GenshinInfo::getUid, info.getUid()).set(GenshinInfo::getCookie, info.getCookie());
            genshinService.update(updateWrapper);
        }

    }

    @OnPrivate
    @OnlySession(group = GENSHIN_SIGN)
    public void getCookie(PrivateMsg privateMsg, MsgSender sender, ListenerContext context) {

        final ContinuousSessionScopeContext session = (ContinuousSessionScopeContext) context.getContext(ListenerContext.Scope.CONTINUOUS_SESSION);

        MsgSenderHelper senderHelper = MsgSenderHelper.getInstance(privateMsg, sender);

        // ????????????????????????????????????key
        String key = KEY_START + ":" + senderHelper.getQqNumber();

        session.push(GENSHIN_SIGN, key, privateMsg.getText());

        senderHelper.PRIVATE.sendMsg("????????????cookie???????????????cookie?????????");
    }
    @Filter(value = "^[1-5]\\d{8}", matchType = MatchType.REGEX_MATCHES)
    @OnPrivate
    @OnlySession(group = GENSHIN_SIGN_CHOOSE_UID)
    public void getChooseUID(PrivateMsg privateMsg, MsgSender sender, ListenerContext context) {

        final ContinuousSessionScopeContext session = (ContinuousSessionScopeContext) context.getContext(ListenerContext.Scope.CONTINUOUS_SESSION);

        MsgSenderHelper senderHelper = MsgSenderHelper.getInstance(privateMsg, sender);

        // ????????????????????????????????????key
        String key = KEY_START + ":" + senderHelper.getQqNumber();

        session.push(GENSHIN_SIGN_CHOOSE_UID, key, privateMsg.getText());

        senderHelper.PRIVATE.sendMsg("???????????????????????????????????????");
    }

}
