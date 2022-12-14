package com.zsck.bot.http.mihoyo.sign.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.zsck.bot.http.mihoyo.sign.enums.ServerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author QQ:825352674
 * @date 2022/8/31 - 22:50
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class GenshinInfo {
    @TableId
    private Integer id;
    private String uid;
    private String qqNumber;
    private String nickName;
    private String cookie;
    private Integer push;
    private Integer deletes;

    @TableField(exist = false)
    private ServerType serverType;

    public void setUid(String uid) {
        serverType = uid.charAt(0) < '5' ? ServerType.OFFICIAL : ServerType.FOREIGN;
        this.uid = uid;
    }
}
