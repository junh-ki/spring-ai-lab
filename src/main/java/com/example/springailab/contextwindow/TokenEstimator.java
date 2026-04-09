package com.example.springailab.contextwindow;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.ModelType;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@NoArgsConstructor
public class TokenEstimator {

    private final Encoding encoding = Encodings.newDefaultEncodingRegistry()
        .getEncodingForModel(
            ModelType.GPT_4
        );

    public int estimate(final String text) {
        if (StringUtils.isBlank(text)) {
            return 0;
        }
        return this.encoding.countTokens(text);
    }
}
