package com.zsck.bot.http.kugou.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author QQ:825352674
 * @date 2022/8/12 - 9:41
 */
@NoArgsConstructor
@Data
public class MusicDetail extends Music {
    @JsonProperty("timelength")
    private Integer time;
    private String hash;
    @JsonProperty("filesize")
    private Integer fileSize;
    private Integer haveAlbum;
    private String albumName;
    private String albumId;
    private String haveMv;
    private String videoId;
    private String lyrics;
    private String authorId;
    private Integer privilege;
    private String privilege2;
    private String playUrl;
    private List<Author> authors;
    private Integer isFreePart;
    private Integer bitrate;
    private String recommendAlbumId;
    private String storeType;
    private String albumAudioId;
    private Boolean hasPrivilege;
    @JsonProperty("show_tips")
    private String tip;

}
