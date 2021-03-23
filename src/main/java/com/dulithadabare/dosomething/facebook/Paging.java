package com.dulithadabare.dosomething.facebook;

public class Paging
{
    private Cursors cursors;
    private String previous;
    private String next;

    public Paging()
    {
    }

    public Cursors getCursors()
    {
        return cursors;
    }

    public void setCursors( Cursors cursors )
    {
        this.cursors = cursors;
    }

    public String getPrevious()
    {
        return previous;
    }

    public void setPrevious( String previous )
    {
        this.previous = previous;
    }

    public String getNext()
    {
        return next;
    }

    public void setNext( String next )
    {
        this.next = next;
    }
}
