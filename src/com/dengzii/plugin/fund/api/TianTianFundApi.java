package com.dengzii.plugin.fund.api;

import com.dengzii.plugin.fund.api.bean.FundBean;
import com.dengzii.plugin.fund.api.bean.NetValueBean;
import com.dengzii.plugin.fund.http.Http;
import com.dengzii.plugin.fund.utils.GsonUtils;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TianTianFundApi implements FundApi {

    @Override
    public List<FundBean> getFundList() {
        try {
            String response = Http.getInstance().get("http://fund.eastmoney.com/js/fundcode_search.js").trim();
            if (response.isBlank()) {
                return Collections.emptyList();
            }
            response = response.replaceAll("(var r = \\[)|( )|(];)|(\")", "");
            response = response.substring(2, response.length() - 1);
            String[] fund = response.split("],\\[");
            List<FundBean> fundBeans = new ArrayList<>();
            for (String f : fund) {
                String[] sp = f.split(",");
                FundBean fb = new FundBean();
                fb.setFundCode(sp[0]);
                fb.setPingYingAbbr(sp[1]);
                fb.setFundName(sp[2]);
                fb.setTypeName(sp[3]);
                fb.setPingYing(sp[4]);
                fundBeans.add(fb);
            }
            return fundBeans;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    @Override
    public FundBean getFundNewestDetail(String fundCode) {
        try {
            String response = Http.getInstance().get("http://fundgz.1234567.com.cn/js/001186.js?rt=" + System.currentTimeMillis());
            response = response.substring(8, response.length() - 2);
            Type t = new TypeToken<FundBean>() {
            }.getType();
            return GsonUtils.fromJson(response, t);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public NetValueBean getNewestNetValue(String fundCode) {
        List<NetValueBean> h = getNetValueHistory(fundCode, 1, 1);
        if (h.isEmpty()) {
            return null;
        }
        return h.get(0);
    }

    @Override
    public List<NetValueBean> getNetValueHistory3Month(String fundCode) {

        List<NetValueBean> n = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            n.addAll(getNetValueHistory(fundCode, 1, 49));
        }
        return n;
    }

    @Override
    public List<NetValueBean> getNetValueHistory(String fundCode, int page, int pageSize) {
        String url = String.format("https://fundf10.eastmoney.com/F10DataApi.aspx?type=lsjz&code=%s&page=%d&per=%d",
                fundCode, page, pageSize);
        List<NetValueBean> netValueBeans = new ArrayList<>();
        try {
            String response = Http.getInstance().get(url);
            response = response.replaceAll("(var apidata=\\{ content:\")|(\",records:1748,pages:36,curpage:1};)", "");
            response = response.substring(187);
            response = response.substring(0, response.length() - 21);
            response = response.replaceAll("(<td>)|" +
                    "(</td><td class='tor bold'>)|" +
                    "(</td><td class='tor bold (grn|red)'>)|" +
                    "(</td><td>)|" +
                    "(</td><td class='red unbold'></td>)", ",");
            String[] lines = response.split("</tr><tr>");
            for (String line : lines) {
                String[] sp = line.split(",");
                NetValueBean bean = new NetValueBean();
                bean.setDate(sp[1]);
                bean.setNetValue(parseFloat(sp[2]));
                bean.setGrowthRate(parseFloat(sp[4]));
                bean.setSubscribeStatus(sp[5]);
                bean.setRedemptionStatus(sp[6]);
                netValueBeans.add(bean);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return netValueBeans;
    }

    private float parseFloat(String s) {
        try {
            return Float.parseFloat(s.replace("%", ""));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return 0;
    }
}
