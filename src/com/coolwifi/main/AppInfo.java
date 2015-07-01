package com.coolwifi.main;

import android.content.pm.ProviderInfo;
import android.graphics.drawable.Drawable;
 
public class AppInfo {
      private int versionCode = 0;  //版本号 
      private String appname = "";// 程序名称
      private String packagename = "";    //包名称
      private Drawable appicon = null;//图标
      private long lastInstal;//最后一次安装时间
      private ProviderInfo[] provider;//供应商
      private String InstalPath;//安装路径
     
    public int getVersionCode() {
        return versionCode;
    }
     
    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }
     
    public String getAppname() {
        return appname;
    }
     
    public void setAppname(String appname) {
        this.appname = appname;
    }
     
    public String getPackagename() {
        return packagename;
    }
     
    public void setPackagename(String packagename) {
        this.packagename = packagename;
    }
     
     
     
    public Drawable getAppicon() {
        return appicon;
    }
     
    public void setAppicon(Drawable appicon) {
        this.appicon = appicon;
    }
 
    /**
     * @return the lastInstal
     */
    public long getLastInstal() {
        return lastInstal;
    }
 
    /**
     * @param firstInstallTime the lastInstal to set
     */
    public void setLastInstal(long firstInstallTime) {
        this.lastInstal = firstInstallTime;
    }
 
    /**
     * @return the provider
     */
    public ProviderInfo[] getProvider() {
        return provider;
    }
 
    /**
     * @param providers the provider to set
     */
    public void setProvider(ProviderInfo[] providers) {
        this.provider = providers;
    }
 
    /**
     * @return the instalPath
     */
    public String getInstalPath() {
        return InstalPath;
    }
 
    /**
     * @param instalPath the instalPath to set
     */
    public void setInstalPath(String instalPath) {
        InstalPath = instalPath;
    }
}
