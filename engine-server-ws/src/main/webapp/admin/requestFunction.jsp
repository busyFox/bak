<%--
  Created by IntelliJ IDEA.
  User: pserent
  Date: 25/11/2015
  Time: 12:18
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%!
    /**
     * Convert int value to string hexa
     * @param value must be > 0
     * @return
     */
    public static String intToHexaString(int value) {
        StringBuilder sb = new StringBuilder();
        sb.append(Integer.toHexString(value));
        if (sb.length() %2 == 1) {
            sb.insert(0, "0");
        }
        return sb.toString().toUpperCase();
    }

    /**
     * Build options according to result type and engine version
     * @param resultType
     * @param engineVersion
     * @param engineSel 0 by default
     * @param engineSpeed 0 by default
     * @param spreadEnable 0 by default
     * @return
     */
    public static String buildRequestOptions(int resultType, int engineVersion, int engineSel, int engineSpeed, int spreadEnable) {
        // options field : 0=engine selection - 1=engine speed - 2=result type - 3&4=engine version
        String result = "";
        result += intToHexaString(engineSel & 0xFF);
        result += intToHexaString(engineSpeed & 0xFF);
        result += intToHexaString(resultType & 0xFF);
        result += intToHexaString(engineVersion & 0xFF); // lowbyte
        result += intToHexaString((engineVersion & 0xFF00) >> 8); // highbyte
        result += intToHexaString(spreadEnable & 0xFF);
        return result;
    }
%>
