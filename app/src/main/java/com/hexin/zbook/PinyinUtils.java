    package com.hexin.zbook;

    import net.sourceforge.pinyin4j.PinyinHelper;
    import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
    import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;    import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
    import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

    public class PinyinUtils {

        /**
         * 获取中文字符串的拼音首字母, 英文字符不变
         *
         * @param chinese 汉字串
         * @return 汉语拼音首字母
         */
        public static String getFirstSpell(String chinese) {
            if (chinese == null) {
                return null;
            }
            StringBuilder pybf = new StringBuilder();
            char[] arr = chinese.toCharArray();
            HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
            defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
            defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
            for (char c : arr) {
                if (c > 128) { // 如果是汉字
                    try {
                        String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, defaultFormat);
                        if (pinyinArray != null && pinyinArray.length > 0) {
                            pybf.append(pinyinArray[0].charAt(0));
                        }
                    } catch (BadHanyuPinyinOutputFormatCombination e) {
                        e.printStackTrace();
                    }
                } else { // 如果是 ASCII 字符
                    pybf.append(c);
                }
            }
            return pybf.toString();
        }
    }
    