package onl.tesseract;

import java.util.HashSet;
import java.util.Set;

public class Guild {
    public static Set<Guild> guilds = new HashSet<>();

    public static Guild forName(String input)
    {
        for (Guild guild : guilds)
        {
            if (input.equals(guild.getName()))
                return guild;
        }
        return null;
    }

    public String getName()
    {
        return null;
    }

    public HashSet<Parcel> getParcels()
    {
        return null;
    }
}
