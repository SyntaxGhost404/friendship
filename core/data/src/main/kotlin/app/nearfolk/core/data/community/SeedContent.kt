package app.nearfolk.core.data.community

import app.nearfolk.core.model.Gender
import app.nearfolk.core.model.Mood

/**
 * Original sample community used until a backend is connected. Every name,
 * line and caption here was written for this app.
 */
internal object SeedContent {

    data class Person(
        val name: String,
        val gender: Gender,
        val country: String,
        val city: String,
        val birth: String,
        val catchphrase: String,
        val bio: String,
    )

    val people = listOf(
        Person("Amara Okafor", Gender.Female, "NG", "Lagos", "1997-08-14", "Learning to enjoy my own company", "Product designer by day. Slow cooking, old highlife records and long phone calls by night. Looking for people who like real conversations."),
        Person("Lucas Ferreira", Gender.Male, "BR", "Porto Alegre", "1994-03-02", "Coffee first, then everything else", "Moved cities last year and still finding my rhythm. I run at sunrise and read too many history books."),
        Person("Noor Abadi", Gender.Female, "AE", "Abu Dhabi", "1999-11-23", "Quiet, not shy", "Architecture student who sketches buildings nobody notices. Tell me about your favourite street."),
        Person("Søren Holm", Gender.Male, "DK", "Aarhus", "1991-01-17", "Cold water swimmer, warm person", "Carpenter. I build furniture and sometimes friendships. Winter swimming keeps me honest."),
        Person("Mei Lin", Gender.Female, "SG", "Singapore", "1996-06-09", "Night owl with a day job", "Data analyst who collects houseplants faster than I can name them. Big fan of late night noodle runs."),
        Person("Tomás Rivera", Gender.Male, "MX", "Guadalajara", "1993-09-30", "Playing guitar badly, daily", "Teacher, amateur guitarist, devoted uncle. I like people who laugh at their own jokes."),
        Person("Inês Duarte", Gender.Female, "PT", "Lisbon", "1998-04-12", "Tram rides and tangerines", "Translator working between four languages. Ask me for a word and I will give you three."),
        Person("Kai Nakamura", Gender.Nonbinary, "JP", "Osaka", "2000-02-27", "Film camera, no filter", "I shoot on film and wait weeks to see what I got. Patience is my favourite hobby."),
        Person("Hana Kowalska", Gender.Female, "PL", "Kraków", "1995-12-05", "Here for honest talk", "Nurse on night shifts. When I am awake at odd hours, I like knowing someone else is too."),
        Person("Jonah Weiss", Gender.Male, "DE", "Leipzig", "1990-07-21", "Recovering workaholic", "Software engineer trying to have hobbies that do not involve screens. Currently: bread, bouldering, board games."),
        Person("Priya Raman", Gender.Female, "IN", "Chennai", "1997-10-03", "Filter coffee evangelist", "Researcher in marine biology. I will absolutely tell you facts about octopuses if you let me."),
        Person("Mateo Silva", Gender.Male, "AR", "Rosario", "1996-05-18", "Mate in hand, always", "Football on Sundays, cooking for friends on Fridays. New to living alone and learning a lot."),
        Person("Elif Demir", Gender.Female, "TR", "Izmir", "1998-01-29", "Sea air fixes most things", "Illustrator. I draw cats, ferries and the people who sit next to me on them."),
        Person("Rafael Costa", Gender.Male, "BR", "Recife", "1992-11-11", "Small talk is fine, big talk is better", "Physiotherapist and weekend surfer. I like slow mornings and fast friendships."),
        Person("Zainab Yusuf", Gender.Female, "KE", "Nairobi", "1999-07-07", "Building a softer routine", "Studying public health. Writing a journal every night for a year, day 214 now."),
        Person("Théo Laurent", Gender.Male, "FR", "Lyon", "1995-02-14", "Born on Valentine's, still single", "Pastry cook. I wake up at four and I am very good at croissants and very bad at texting back. Working on it."),
        Person("Ana Petrović", Gender.Female, "RS", "Novi Sad", "1994-08-25", "Bookshop regular", "Librarian who recommends books whether you ask or not. Let us trade favourites."),
        Person("Arjun Mehta", Gender.Male, "IN", "Pune", "1998-09-15", "Cricket, chai, conversation", "Final year engineering student. Trying to talk to more people outside my bubble."),
        Person("Lena Fischer", Gender.Female, "AT", "Graz", "1993-04-04", "Mountains on weekends", "Veterinarian. I hike, I knit, I talk to every dog I meet."),
        Person("Sami Haddad", Gender.Male, "JO", "Amman", "1997-06-21", "Listening more than talking", "Sound engineer. I record city noise and turn it into calm playlists."),
        Person("Rowan Price", Gender.Nonbinary, "GB", "Bristol", "1996-10-31", "Tea, rain, repeat", "Bookbinder and part time barista. Gentle chats appreciated, puns tolerated."),
        Person("Chloé Martin", Gender.Female, "CA", "Montréal", "1995-03-08", "Winter is a personality", "Urban planner who loves bike lanes far too much. Ask me about my favourite bench."),
        Person("Diego Navarro", Gender.Male, "ES", "Valencia", "1991-12-19", "Paella opinions available on request", "Chef. Moved back home after ten years abroad and reconnecting with the city and with myself."),
        Person("Yuna Park", Gender.Female, "KR", "Busan", "2000-05-01", "Ocean view, inner calm", "Graphic design student. I make zines about food and friendship."),
        Person("Oskar Lind", Gender.Male, "SE", "Malmö", "1994-02-10", "Introvert in training", "Game tester and amateur birdwatcher. I like conversations that drift."),
        Person("Fatima Zahra Benali", Gender.Female, "MA", "Rabat", "1996-09-09", "Mint tea and long letters", "Architect. I still write letters by hand and would love a pen pal who writes back."),
        Person("Marcus Bell", Gender.Male, "US", "Portland", "1990-05-27", "Day 300 of trying new things", "Mechanic and part time drummer. Divorced, rebuilding, optimistic."),
        Person("Isabela Gomes", Gender.Female, "BR", "Curitiba", "1999-12-24", "Plant mom, cat aunt", "Biology student. Ask me which tree is outside your window and I will try to guess."),
        Person("Ilja Tamm", Gender.Male, "EE", "Tallinn", "1995-08-08", "Saunas and synthesizers", "Electronic musician and backend developer. I like quiet places and loud music."),
        Person("Aoife Byrne", Gender.Female, "IE", "Galway", "1997-03-17", "Sea swim survivor", "Primary school teacher. Kids tell me I am funny and I have decided to believe them."),
        Person("Kwame Mensah", Gender.Male, "GH", "Kumasi", "1993-10-10", "Good energy only", "Accountant who dances at weddings like nobody is watching. Everyone is watching."),
        Person("Sofia Romano", Gender.Female, "IT", "Bologna", "1998-07-30", "Pasta is a love language", "Medical resident. Not much free time, so I make it count."),
        Person("Linh Trần", Gender.Female, "VN", "Da Nang", "1999-04-19", "Scooter rides at dusk", "Hotel receptionist learning Korean and pottery at the same time."),
        Person("Ethan Brooks", Gender.Male, "AU", "Hobart", "1996-01-05", "Far from everywhere, close to nature", "Park ranger in Tasmania. My nearest neighbour is a wombat."),
        Person("Maya Cohen", Gender.Female, "CA", "Vancouver", "1994-11-02", "Rain jacket, always", "Therapist. Off the clock I like terrible movies and great company."),
        Person("Juniper Hale", Gender.Nonbinary, "US", "Asheville", "1998-06-13", "Growing things, mostly myself", "Gardener and ceramicist. Happy to swap seeds, playlists or stories."),
        Person("Nikos Papadakis", Gender.Male, "GR", "Thessaloniki", "1992-04-23", "Old songs, new friends", "History teacher. Every street here has a story and I know most of them."),
        Person("Camila Ortiz", Gender.Female, "CO", "Medellín", "1997-02-02", "Salsa on Thursdays", "Software tester and dance student. I believe every friendship starts with a good question."),
        Person("Bram de Vries", Gender.Male, "NL", "Utrecht", "1995-09-01", "Cycling through it", "Urban farmer. Ask me about growing tomatoes on a balcony."),
        Person("Leila Karimi", Gender.Female, "SE", "Gothenburg", "1996-12-12", "Soft heart, strong coffee", "Pharmacist and amateur baker. New in town, would love recommendations and company."),
    )

    val captions = listOf(
        "Took the long way home today. No regrets.",
        "First time cooking this recipe. It looked nothing like the photo but tasted right.",
        "Morning light through the kitchen window. Small things.",
        "Spent the whole afternoon reading. Phone on silent. Highly recommend.",
        "Found this spot by accident. Coming back next week.",
        "Day off. Did absolutely nothing and I am proud of it.",
        "New plant. Her name is Agnes.",
        "Rain all day, so I finally sorted my photos from last year.",
        "Walked past the same café for months. Finally went in. Good decision.",
        "Tried a pottery class. Made one bowl and one very confident lump.",
        "Sunset from the roof. Neighbours invited me up, which was kind.",
        "Ten kilometres this morning. Legs disagree with this choice.",
        "Market haul for the week. Too many lemons, as usual.",
        "Note to self: call people back sooner.",
        "This bench has the best view in the city and nobody sits on it.",
        "Learning to enjoy eating alone at restaurants. It is getting easier.",
        "The bus was late, so I talked to a stranger for twenty minutes. Lovely person.",
        "Weekend project finished. Shelf is crooked but it holds books.",
        "Back to journaling after a long break. It helps.",
        "Fresh bread at 7 am. Worth the alarm.",
        "Old friends, new city, same jokes.",
        "Saying yes to more invitations this month. Report to follow.",
        "Quiet evening, good tea, playlist on repeat.",
        "Somebody left flowers on the train seat. Made my whole day.",
        "Hiked up early to beat the crowds. Had the summit to myself.",
        "Rearranged my room. Feels like a new apartment.",
        "Cooking for one is just cooking for future you.",
        "Today's small win: I said hello first.",
        "The sea was calm, so I was too.",
        "Library card renewed. Dangerous amounts of books incoming.",
    )

    val comments = listOf(
        "This looks so peaceful.",
        "Where is this? I need to go.",
        "Love this.",
        "Agnes is beautiful.",
        "Proud of you for this!",
        "Same energy today.",
        "The colours here are lovely.",
        "Okay this made me smile.",
        "Recipe please?",
        "Totally agree.",
        "I needed to read this today.",
        "That sounds like a perfect day.",
    )

    /** Answers in a comment thread, to the comment or to someone else's reply. */
    val replies = listOf(
        "Same here, every single week.",
        "Ha, exactly this.",
        "You should go back in spring, it is even better.",
        "Right? I keep thinking about it.",
        "Thank you, that means a lot.",
        "Saving this for later.",
        "How did you find it?",
        "Count me in next time.",
        "That made my morning.",
        "Agreed, the light was perfect.",
        "Stealing this idea.",
        "I said the same thing to a friend yesterday.",
        "Took me a while to get there too.",
        "Fair point, I had not thought of it that way.",
    )

    data class JournalSeed(val title: String, val body: String, val mood: Mood)

    val journals = listOf(
        JournalSeed("Notes from a slow Sunday", "Woke up without an alarm and stayed in bed reading for an hour. Called my sister. Made soup. I keep thinking that being alone is not the same as being lonely, and today proved it.", Mood.Calm),
        JournalSeed("Three good things", "1. The baker remembered my order.\n2. I finished the book I abandoned in spring.\n3. A stranger on here asked me a real question and I answered honestly.", Mood.Grateful),
        JournalSeed("Rough week", "Work was heavy and I did not reach out to anyone. Writing it down so I notice the pattern. Next week I want to send at least one message a day, even a small one.", Mood.Low),
        JournalSeed("Trying something new", "Signed up for an evening class. I am nervous about walking into a room full of strangers, but I did it here first, so maybe it is practice.", Mood.Hopeful),
        JournalSeed("Can't sleep", "Mind racing about everything I should have said this year. Writing until it slows down. Tomorrow I will go for a walk before checking my phone.", Mood.Restless),
        JournalSeed("On living alone", "Six months in my own place. I have learned that I like silence in the morning and noise in the evening. I am slowly building a life that fits me.", Mood.Hopeful),
    )

    /** Openers from members who start a conversation with you. */
    val introOpeners = listOf(
        "Hi! Your profile made me smile. How is your week going?",
        "Hey, I saw you just joined. Welcome! What brought you here?",
        "Hello from %s. What is the weather like where you are?",
        "Hi there. I like your status. Mine is just coffee related, as you can see.",
        "Hey! Random question: what is the best thing you ate this week?",
    )

    val greetingReplies = listOf(
        "Hey! Good to hear from you.",
        "Hi! Thanks for saying hello. How is your day going?",
        "Hello! I was hoping someone would write today.",
        "Hey there. Nice to meet you.",
    )

    val questionReplies = listOf(
        "Good question. Honestly, mostly work and long walks lately. What about you?",
        "I think so, yes. It depends on the day though.",
        "Hmm, let me think. Probably cooking something new on the weekend. You?",
        "Not much, just trying to keep a calm routine. How about you?",
        "That is a hard one. I will say yes for now and change my mind later.",
    )

    val generalReplies = listOf(
        "That makes sense.",
        "Ha, I know that feeling.",
        "Tell me more about that.",
        "I like that. What got you into it?",
        "Same here, more or less.",
        "That sounds like a good way to spend a weekend.",
        "Honestly, that is really nice to hear.",
        "Oh nice! I have always wanted to try that.",
        "Fair point. I had not thought about it like that.",
        "I am glad you told me.",
    )

    /** Answers to a message that is only a photo, video or GIF. */
    val mediaReplies = listOf(
        "Oh, I like that.",
        "Ha, that made me smile.",
        "Where was this?",
        "That looks lovely.",
        "Okay, now I want to see more.",
    )

    val greetingWords = setOf("hi", "hey", "hello", "hola", "hallo", "ciao", "salut", "yo", "morning", "evening")
}
