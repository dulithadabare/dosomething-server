package com.dulithadabare.dosomething.facebook;

import java.time.LocalDate;

public class PublicProfile
{
    private String id;
    private String first_name;
    private String last_name;
    private String middle_name;
    private String name;
    private String email;
    private String birthday;
    private String name_format;
    private String picture;
    private String short_name;

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getFirst_name()
    {
        return first_name;
    }

    public void setFirst_name( String first_name )
    {
        this.first_name = first_name;
    }

    public String getLast_name()
    {
        return last_name;
    }

    public void setLast_name( String last_name )
    {
        this.last_name = last_name;
    }

    public String getMiddle_name()
    {
        return middle_name;
    }

    public void setMiddle_name( String middle_name )
    {
        this.middle_name = middle_name;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }

    public String getBirthday()
    {
        return birthday;
    }

    public void setBirthday( String birthday )
    {
        this.birthday = birthday;
    }

    public String getName_format()
    {
        return name_format;
    }

    public void setName_format( String name_format )
    {
        this.name_format = name_format;
    }

    public String getPicture()
    {
        return picture;
    }

    public void setPicture( String picture )
    {
        this.picture = picture;
    }

    public String getShort_name()
    {
        return short_name;
    }

    public void setShort_name( String short_name )
    {
        this.short_name = short_name;
    }

    public LocalDate getBirthdayAsLocalDate()
    {
        return LocalDate.parse( this.birthday );
    }
}
