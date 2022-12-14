package com.zsck.bot.http.kugou.listener;

import catcode.CatCodeUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.zsck.bot.common.permit.annotation.BotPermits;
import com.zsck.bot.common.permit.enums.Permit;
import com.zsck.bot.helper.MsgSenderHelper;
import com.zsck.bot.http.kugou.HttpMusicSender;
import com.zsck.bot.http.kugou.KuGouMusic;
import com.zsck.bot.http.kugou.pojo.Music;
import com.zsck.bot.http.kugou.service.MusicService;
import kotlinx.coroutines.TimeoutCancellationException;
import lombok.extern.slf4j.Slf4j;
import love.forte.simbot.annotation.*;
import love.forte.simbot.api.message.MessageContentBuilder;
import love.forte.simbot.api.message.MessageContentBuilderFactory;
import love.forte.simbot.api.message.events.GroupMsg;
import love.forte.simbot.api.message.events.MessageGet;
import love.forte.simbot.api.message.results.FileInfo;
import love.forte.simbot.api.message.results.FileResult;
import love.forte.simbot.api.sender.AdditionalApi;
import love.forte.simbot.api.sender.MsgSender;
import love.forte.simbot.component.mirai.additional.MiraiAdditionalApis;
import love.forte.simbot.filter.MatchType;
import love.forte.simbot.listener.ContinuousSessionScopeContext;
import love.forte.simbot.listener.ListenerContext;
import love.forte.simbot.listener.SessionCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author QQ:825352674
 * @date 2022/7/28 - 22:30
 */
@Slf4j
@DependsOn({"kuGouMusic","contextUtil"})
@ListenGroup("music")
@Controller
public class KuGouListener {
    private final CatCodeUtil codeUtil = CatCodeUtil.getInstance();
    @Autowired
    private MusicService musicService;
    @Autowired
    private KuGouMusic kuGouMusic;
    @Autowired
    private HttpMusicSender musicSender;
    @Autowired
    private MessageContentBuilderFactory factory;


    private final static String KEY_START = "==KEY_START==";
    private final static String KUGOU_MUSIC = "KUGOU_MUSIC:UPLOAD_FILE";
    private final static String KUGOU_CHOOSE_MUSIC = "KUGOU_MUSIC:KUGOU_CHOOSE_MUSIC";


    @Filter(value = "^/??????\\s*({{param,(-d|D){0,1}}})\\s*{{keyword}}" , matchType = MatchType.REGEX_MATCHES)
    @OnGroup
    public void getRandom(GroupMsg groupMsg, MsgSender sender,
                          ListenerContext context,
                          @FilterValue("param") String param,
                          @FilterValue("keyword") String keyword){
        MsgSenderHelper senderHelper = MsgSenderHelper.getInstance(groupMsg, sender);
        AtomicReference<Music> desMusic = new AtomicReference<>();
        List<Music> localMusic = musicService.likeMusic(keyword);
        if (StrUtil.isBlankOrUndefined(param)){
            if (!localMusic.isEmpty()) {
                desMusic.set(localMusic.get(0));
            }

            if (desMusic.get() == null) {//???????????????????????????????????????
                log.info("?????????: {}???????????????????????????????????????", keyword);
                desMusic.set(kuGouMusic.getOneMusicForUrl(keyword));
            }

            if (desMusic.get().getImgUrl() == null) {
                desMusic.get().setImgUrl(groupMsg.getAccountInfo().getAccountAvatar());
            }

            senderHelper.GROUP.sendMsg(getKuGouMsg(desMusic.get()));
        }else {
            List<Music> netMusic = kuGouMusic.getFileNames(keyword, 5);
            MessageContentBuilder builder = factory.getMessageContentBuilder();
            builder.text("?????????:" + keyword + ",????????????:\n????????????:");
            int i = 0;
            for (Music music : netMusic){
                i++;
                builder.text("\n" + i + ". " + music.getAudioName());
                music.setId(i);
            }

            builder.text("\n????????????:");
            if (localMusic.isEmpty()){
                builder.text("\n??????????????????");
            }else {

                for (Music music : localMusic) {
                    i++;
                    if (i >= 8){
                        break;
                    }
                    builder.text("\n" + i + ". " + music.getAudioName());
                    music.setId(i);
                }
            }

            senderHelper.GROUP.sendMsg(builder.build());
            ContinuousSessionScopeContext scopeContext = (ContinuousSessionScopeContext)context.getContext(ListenerContext.Scope.CONTINUOUS_SESSION);

            SessionCallback<Integer> callback = SessionCallback.builder(Integer.class).onResume(id ->{

                netMusic.addAll(localMusic);
                for (Music music : netMusic){
                    if (music.getId().equals(id)){
                        desMusic.set(music);
                    }
                }

                if (desMusic.get() != null) {
                    if (desMusic.get().getUrl() == null){
                        desMusic.set( kuGouMusic.getMusicUrlByAlbumIDAndHash( desMusic.get() ) );
                    }

                    if (desMusic.get().getImgUrl() == null) {
                        desMusic.get().setImgUrl(groupMsg.getAccountInfo().getAccountAvatar());
                    }

                    senderHelper.GROUP.sendMsg(getKuGouMsg(desMusic.get()));
                }else {
                    senderHelper.GROUP.sendMsg("id??????");
                }

            } ).onError(exception -> {
                if ( !(exception instanceof TimeoutCancellationException)){
                    senderHelper.GROUP.sendMsg("????????????");
                    exception.printStackTrace();
                }
            }).build();

            String key = KEY_START + senderHelper.getQqNumber() + KUGOU_CHOOSE_MUSIC;

            scopeContext.waiting(KUGOU_CHOOSE_MUSIC, key, 60000, callback);
        }
    }



    private String getKuGouMsg(Music music) {
        return "[CAT:other,code=&#91;mirai:origin:MUSIC_SHARE&#93;][CAT:music,kind=kugou," +
                "musicUrl=" + music.getUrl() +//mp3??????url
                ",title=" + music.getTitle() +
                ",jumpUrl=https://www.kugou.com/song/#hash&#61;&amp;album_id&#61;48534841," +
                "pictureUrl=" + music.getImgUrl() + ",summary=" + music.getAudioName() + ",brief=&#91;??????&#93;" +
                music.getAudioName() + "]";

    }

    @Filter(value = "[1-8]", matchType = MatchType.REGEX_MATCHES)
    @OnlySession(group = KUGOU_CHOOSE_MUSIC)
    @OnGroup
    public void getMusicId(GroupMsg msgGet, ListenerContext context){
        ContinuousSessionScopeContext scopeContext = (ContinuousSessionScopeContext)context.getContext(ListenerContext.Scope.CONTINUOUS_SESSION);
        String key = KEY_START + msgGet.getAccountInfo().getAccountCode() + KUGOU_CHOOSE_MUSIC;

        scopeContext.push(KUGOU_CHOOSE_MUSIC, key, Integer.parseInt(msgGet.getText()));

    }
    @OnlySession(group = KUGOU_MUSIC)
    @Filters(customFilter = "catFilter")
    @OnGroup
    public void setMP3File(MessageGet msgGet, MsgSender sender, ListenerContext context){
        ContinuousSessionScopeContext scopeContext = (ContinuousSessionScopeContext)context.getContext(ListenerContext.Scope.CONTINUOUS_SESSION);
        MsgSenderHelper senderHelper = MsgSenderHelper.getInstance(msgGet, sender);//??????(??????|??????)???????????????
        String key = KEY_START + senderHelper.getQqNumber() + KUGOU_MUSIC;

        String id = codeUtil.getParam(msgGet.getMsg(), "id");
        if (id != null) {
            AdditionalApi<FileResult> fileApi = MiraiAdditionalApis.GETTER.getGroupFileById(senderHelper.getGroup(), id,  true);

            FileResult fileRes = sender.GETTER.additionalExecute(fileApi);
            //??????????????????

            log.info("???????????????: {}", fileRes.getValue().getName());
            senderHelper.GROUP.sendMsg("?????????????????????:" + fileRes.getValue().getName());

            scopeContext.push(KUGOU_MUSIC, key, fileRes.getValue());

        }
    }

    @BotPermits(Permit.MANAGER)//??????: /?????? love story - TaylorSwifter
    @Filter(value = "/????????????" , matchType = MatchType.EQUALS)
    @OnGroup
    public void setMP3Detail(GroupMsg groupMsg, MsgSender sender, ListenerContext context){
        ContinuousSessionScopeContext scopeContext = (ContinuousSessionScopeContext)context.getContext(ListenerContext.Scope.CONTINUOUS_SESSION);
        MsgSenderHelper senderHelper = MsgSenderHelper.getInstance(groupMsg, sender);

        String key = KEY_START + senderHelper.getQqNumber() + KUGOU_MUSIC;

        senderHelper.GROUP.sendMsg("?????????MP3???flac??????????????????zip?????????");
        SessionCallback<FileInfo> callback = SessionCallback.builder(FileInfo.class).onResume(file ->{
            MessageContentBuilder builder = factory.getMessageContentBuilder();

            //????????????mp3?????????mp3?????????zip????????????????????????????????????
            JsonNode jsonNode = musicSender.sendMusicDetail(file.getUrl(), file.getName());
            jsonNode.forEach( node-> builder.text(node.asText() + "\n"));

            senderHelper.GROUP.sendMsg(builder.build());

        } ).onError(exception -> {
            if ( !(exception instanceof TimeoutCancellationException)){
                senderHelper.GROUP.sendMsg("????????????");
                exception.printStackTrace();
            }
        }).build();

        scopeContext.waiting(KUGOU_MUSIC, key, 300000, callback);
    }

}
