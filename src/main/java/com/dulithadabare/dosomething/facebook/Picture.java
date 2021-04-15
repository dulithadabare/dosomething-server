package com.dulithadabare.dosomething.facebook;

public class Picture
{
    private int height;
    private int width;
    private boolean isSilhouette;
    private String url;

    public int getHeight()
    {
        return height;
    }

    public void setHeight( int height )
    {
        this.height = height;
    }

    public int getWidth()
    {
        return width;
    }

    public void setWidth( int width )
    {
        this.width = width;
    }

    public boolean isSilhouette()
    {
        return isSilhouette;
    }

    public void setSilhouette( boolean silhouette )
    {
        this.isSilhouette = silhouette;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }
}
