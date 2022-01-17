package com.xatkit.RBAC;

import com.xatkit.core.XatkitBot;
import com.xatkit.plugins.react.platform.ReactPlatform;
import com.xatkit.plugins.react.platform.io.ReactEventProvider;
import com.xatkit.plugins.react.platform.io.ReactIntentProvider;
import lombok.val;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;

import static com.xatkit.dsl.DSL.*;


public class RBACbot{

    public static void main(String[] args) {

        /*
         * RBAC initializations
         */
        //Create roles
        System.out.println("Creating roles...");
        Role freeRole = new Role("FreeRole");
        Role registeredRole = new Role("RegisteredRole");
        //Create actions
        System.out.println("Creating actions...");
        Action matching = new Action("matching", ActionType.MATCHING);
        Action transition = new Action("transition", ActionType.TRANSITION_NAVIGATION);
        //Create resources
        System.out.println("Creating resources...");
        Resource greetingsIntent = new Resource("Greetings",ResourceType.INTENT);
        Resource howIsTheWeatherTodayIntent = new Resource("HowIsTheWeatherToday",ResourceType.INTENT);
        Resource historicalWeatherIntent = new Resource("HistoricalWeather",ResourceType.INTENT);
        //Create policy rules
        System.out.println("Creating policy rules...");
        PolicyRules policyRules = new PolicyRules();
        policyRules.addPermission(freeRole,matching,greetingsIntent);
        policyRules.addPermission(freeRole,matching,howIsTheWeatherTodayIntent);
        policyRules.addPermission(registeredRole,matching,greetingsIntent);
        policyRules.addPermission(registeredRole,matching,howIsTheWeatherTodayIntent);
        policyRules.addPermission(registeredRole,matching,historicalWeatherIntent);
        //Creating the "current" user
        System.out.println("Creating the ''current'' user...");
        User freeTestUser = new User("Elena", freeRole);
        User registeredTestUser = new User("Jordi", registeredRole);
        User currentUser = registeredTestUser;

        /*
         * INTENTS definition
         */
        val greetings = intent("Greetings")
            .trainingSentence("Hi")
            .trainingSentence("Hello")
            .trainingSentence("Good morning")
            .trainingSentence("Good afternoon");

        val howIsTheWeatherToday = intent("HowIsTheWeatherToday")
                .trainingSentence("How is the weather today in CITY?")
                .trainingSentence("What is the forecast for today in CITY?")
                .parameter("cityName").fromFragment("CITY").entity(city());

        val historicalWeather = intent("HistoricalWeather")
                .trainingSentence("How was the weather PERIOD in CITY?")
                .parameter("timePeriod").fromFragment("PERIOD").entity(timePeriod())
                .parameter("cityName").fromFragment("CITY").entity(city());

        ReactPlatform reactPlatform = new ReactPlatform();
        ReactEventProvider reactEventProvider = new ReactEventProvider(reactPlatform);
        ReactIntentProvider reactIntentProvider = new ReactIntentProvider(reactPlatform);

        /*
         * STATES of the bot
         */
        val init = state("Init");
        val awaitingInput = state("AwaitingInput");
        val handleWelcome = state("HandleWelcome");
        val printTodaysWeather = state("PrintTodaysWeather");
        val printHistoricalWeather = state("PrintHistoricalWeather");
        val informAboutPermissions = state("InformAboutPermissions");


        init
                .next()
                    .when(eventIs(ReactEventProvider.ClientReady)).moveTo(awaitingInput);

        awaitingInput
                .next()
                    //move to handledWelcome when the Greetings intent is matched && the user has permission to achieve this intent
                    .when(intentIs(greetings).and(c -> policyRules.checkPermission(currentUser.getRole().getName(),"matching","Greetings"))).moveTo(handleWelcome)
                    //move to printTodaysWeather when the HowIsTheWeatherToday intent is matched && the user has permission to achieve this intent
                    .when(intentIs(howIsTheWeatherToday).and(c -> policyRules.checkPermission(currentUser.getRole().getName(),"matching","HowIsTheWeatherToday"))).moveTo(printTodaysWeather)
                    //move to printHistoricalWeather when the HistoricalWeatherIntent intent is matched && the user has permission to achieve this intent
                    .when(intentIs(historicalWeather).and(c -> policyRules.checkPermission(currentUser.getRole().getName(),"matching","HistoricalWeather"))).moveTo(printHistoricalWeather)
                    //move to informAboutPermissions when the HistoricalWeatherIntent intent is matched && the user has not permission to achieve this intent
                    .when(intentIs(historicalWeather).and(c -> !policyRules.checkPermission(currentUser.getRole().getName(),"matching","HistoricalWeather"))).moveTo(informAboutPermissions);

        handleWelcome
                //This state provides different messages depending on the user role
                .body(context -> {
                    //Welcome message for free users
                    if (currentUser.getRole().getName() == "FreeRole"){
                        reactPlatform.reply(context, "Hi! Welcome to our platform!");
                    }
                    //Welcome message for registered users
                    else if (currentUser.getRole().getName() == "RegisteredRole"){
                        reactPlatform.reply(context, "Hi " + currentUser.getName() + "! Welcome again to our platform!");
                    }
                })
                .next()
                .moveTo(awaitingInput);

        printTodaysWeather
                .body(context -> {
                    reactPlatform.reply(context, "The forecast for today in " + context.getIntent().getValue("cityName") + " is ...");
                    /*String cityName = (String) context.getIntent().getValue("cityName");
                    Map<String, Object> queryParameters = new HashMap<>();
                    queryParameters.put("q", cityName);
                    ApiResponse<JsonElement> response = restPlatform.getJsonRequest(context, "http://api" +
                                    ".openweathermap.org/data/2.5/weather", queryParameters, Collections.emptyMap(),
                            Collections.emptyMap());
                    if (response.getStatus() == 200) {
                        long temp = Math.round(response.getBody().getAsJsonObject().get("main").getAsJsonObject().get(
                                "temp").getAsDouble());
                        long tempMin =
                                Math.round(response.getBody().getAsJsonObject().get("main").getAsJsonObject().get(
                                        "temp_min").getAsDouble());
                        long tempMax =
                                Math.round(response.getBody().getAsJsonObject().get("main").getAsJsonObject().get(
                                        "temp_max").getAsDouble());
                        String weather =
                                response.getBody().getAsJsonObject().get("weather").getAsJsonArray().get(0).getAsJsonObject().get("description").getAsString();
                        String weatherIcon =
                                "http://openweathermap.org/img/wn/" + response.getBody().getAsJsonObject().get(
                                        "weather").getAsJsonArray().get(0).getAsJsonObject().get("icon").getAsString() + ".png";
                        reactPlatform.reply(context, MessageFormat.format("The current weather is {0} &deg;C with " +
                                        "{1} ![{1}]({2}) with a high of {3} &deg;C and a low of {4} &deg;C", temp,
                                weather,
                                weatherIcon, tempMax, tempMin));
                    } else if (response.getStatus() == 400) {
                        reactPlatform.reply(context, "Oops, I couldn't find this city");
                    } else {
                        reactPlatform.reply(context, "Sorry, an error " +  response.getStatus() + " " + response.getStatusText() + " occurred when accessing the openweathermap service");
                    }
                */
                })
                .next()
                .moveTo(awaitingInput);

        printHistoricalWeather
                .body(context -> reactPlatform.reply(context, "The forecast in " + context.getIntent().getValue("cityName") + " during the period " + context.getIntent().getValue("timePeriod") + " is ..."))
                .next()
                .moveTo(awaitingInput);

        informAboutPermissions
                .body(context -> reactPlatform.reply(context, "You must be registered to obtain this information"))
                .next()
                .moveTo(awaitingInput);


        val defaultFallback = fallbackState()
                .body(context -> reactPlatform.reply(context, "Sorry, I didn't, get it"));

        val botModel = model()
                .usePlatform(reactPlatform)
                .listenTo(reactEventProvider)
                .listenTo(reactIntentProvider)
                .initState(init)
                .defaultFallbackState(defaultFallback);

        Configuration botConfiguration = new BaseConfiguration();

        XatkitBot xatkitBot = new XatkitBot(botModel, botConfiguration);
        xatkitBot.run();

    }

}


