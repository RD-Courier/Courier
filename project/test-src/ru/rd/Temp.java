/*
 * Copyright 2005-2017 Courier AUTHORS: please see AUTHORS file.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * 1. Redistributions of source code must retain the above
 *    copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY AUTHORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * <COPYRIGHT HOLDER> OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package ru.rd;

import ru.rd.courier.scripting.*;
import ru.rd.courier.scripting.test.TestContext;
import ru.rd.courier.CourierException;
import ru.rd.courier.utils.FileHelper;
import ru.rd.courier.utils.StringHelper;
import ru.rd.courier.logging.test.NullLogger;

import java.text.SimpleDateFormat;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * User: AStepochkin
 * Date: 22.06.2006
 * Time: 13:25:11
 */
public class Temp {
    public static void main(String[] args) throws CourierException, IOException {
        //final String text = "<root><aaa>1111</aaa><buffer/><bbb>1/17/054</bbb></root>";
        //final String restr = "(?<=<aaa>).*?(?=</aaa>.*<bbb>\\s*1/17/.*?</bbb>)";

        final String text = FileHelper.fileToString(
            new File("O:\\upload\\Message_20060929163601437.xml"), "cp1251"
        );
        final String testr = "[%!replace \n" +
                "            '" + text + "' \n" +
//                "            '(?<=<PointName>).*?(?=</PointName>.*<ClaimNumber>\\s*1/17/.*?</ClaimNumber>)' \n" +
                "            '(?<=<PointName>).*?(?=</PointName>(?s:.*)<ClaimNumber>.*</ClaimNumber>)' \n" +
                "            'ŒŒŒ \"–Œ√¿ »  Œœ€“¿\" (”À.–Œ√¿“¿ﬂ)'\n" +
                "          ]";
        //Pattern reqexp = Pattern.compile(restr);
        //String res = reqexp.matcher(text).replaceAll("4444");

        PreparedTemplate pt = new PreparedTemplate(
            //"[%!replace '" + text + "' '" + restr + "' '4444']"
            testr
        );
        String res = pt.calculate(new TestContext(new NullLogger(), new SimpleDateFormat()));
        System.out.println(res);

        Properties pp = new Properties();
        //StringHelper
    }
}
