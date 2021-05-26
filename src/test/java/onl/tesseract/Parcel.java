package onl.tesseract;

public class Parcel {
    public static Parcel forName(Guild guild, String input)
    {
        for (Parcel p : guild.getParcels())
        {
            if (p.getName().equals(input))
                return p;
        }
        return null;
    }

    public String getName()
    {
        return null;
    }
}
