package idv.hzm.app.admin.mapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

public class MybatisExtendedLanguageDriver extends XMLLanguageDriver implements LanguageDriver {

    /**
     * 支援:
     *   IN (#{ids})
     *   IN ( #{ids} )
     *   NOT IN (#{ids})
     *   NOT IN ( #{ids} )
     */
    private static final Pattern inPattern =
            Pattern.compile("\\(\\s*#\\{(\\w+)\\}\\s*\\)");

    @Override
    public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
        Matcher matcher = inPattern.matcher(script);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String paramName = matcher.group(1);
            String replacement = "(<foreach collection=\"" + paramName +
                    "\" item=\"__item\" separator=\",\" >#{__item}</foreach>)";
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        script = "<script>" + sb.toString() + "</script>";
        return super.createSqlSource(configuration, script, parameterType);
    }
}

