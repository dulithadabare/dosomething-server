package com.dulithadabare.dosomething.model;

import java.util.List;

public class DataListPage<T>
{
    private List<T> itemList;
    private String nextPageKey;

    public List<T> getItemList()
    {
        return itemList;
    }

    public void setItemList( List<T> itemList )
    {
        this.itemList = itemList;
    }

    public String getNextPageKey()
    {
        return nextPageKey;
    }

    public void setNextPageKey( String nextPageKey )
    {
        this.nextPageKey = nextPageKey;
    }
}
