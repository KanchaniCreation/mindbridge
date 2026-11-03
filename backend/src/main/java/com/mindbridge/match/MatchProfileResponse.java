package com.mindbridge.match;

import com.mindbridge.user.User;
import java.util.List;

/** Only ever returned to the person it belongs to (it includes the private real name). */
public record MatchProfileResponse(String alias, String about, String realName, List<Topic> topics,
                                   boolean openToChat) {

    public static MatchProfileResponse from(User user) {
        return new MatchProfileResponse(user.getAlias(), user.getAbout(), user.getRealName(),
            List.copyOf(Topic.parse(user.getTopicsCsv())), user.isOpenToChat());
    }
}
