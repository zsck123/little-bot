package com.zsck.bot.http.kugou.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zsck.bot.http.kugou.pojo.Music;

import java.util.List;

/**
 * @author QQ:825352674
 * @date 2022/8/12 - 21:12
 */
public interface MusicService extends IService<Music> {
    void keepMusic(Music music);

    List<Music> likeMusic(String keyword);
}
