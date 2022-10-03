package com.xatkit.RBAC;

import com.xatkit.core.XatkitBot;
import com.xatkit.plugins.react.platform.ReactPlatform;
import com.xatkit.plugins.react.platform.io.ReactEventProvider;
import com.xatkit.plugins.react.platform.io.ReactIntentProvider;
import lombok.val;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;

import org.casbin.jcasbin.exception.CasbinNameNotExistException;
import org.casbin.jcasbin.model.Assertion;
import org.casbin.jcasbin.model.FunctionMap;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.persist.Adapter;
import org.casbin.jcasbin.persist.file_adapter.FileAdapter;
import org.casbin.jcasbin.rbac.RoleManager;
import org.casbin.jcasbin.main.Enforcer;


import static com.xatkit.dsl.DSL.*;


public class RBACbot{

    public static void main(String[] args) {

        /*
         * INTENTS definition
         */
        val greetings = intent("Greetings")
                .trainingSentence("Hi")
                .trainingSentence("Hey")
                .trainingSentence("Hello")
                .trainingSentence("Good morning")
                .trainingSentence("Good afternoon")
                .trainingSentence("Good afternoon");

        val getProductInformation = intent("GetProductInformation")
                .trainingSentence("I want information about PRODUCT")
                .trainingSentence("I want more information about PRODUCT")
                .trainingSentence("Have you got PRODUCT?")
                .trainingSentence("Do you have PRODUCT?")
                .trainingSentence("I want to see PRODUCT")
                .trainingSentence("Tell me about PRODUCT")
                .parameter("product").fromFragment("PRODUCT").entity(any());

        val trackMyOrder = intent("TrackMyOrder")
                .trainingSentence("I want to see where is the order ID")
                .trainingSentence("Where is the order ID?")
                .trainingSentence("When the order ID will arrive?")
                .parameter("order").fromFragment("ID").entity(number());

        val getEmployeeMonthlyGoals = intent("GetEmployeeMonthlyGoals")
                .trainingSentence("I want to see my goals")
                .trainingSentence("I want to see the progress of my goals")
                .trainingSentence("I want to see the achievement of my goals");

        ReactPlatform reactPlatform = new ReactPlatform();
        ReactEventProvider reactEventProvider = new ReactEventProvider(reactPlatform);
        ReactIntentProvider reactIntentProvider = new ReactIntentProvider(reactPlatform);

        /*
         * POLICY ENFORCEMENT
         */
        Enforcer enforcer = new Enforcer("./src/RBAC/rbac_model.conf", "./src/RBAC/rbac_policy.csv");

        String role = "employee"; // the role of the user that wants to access a resource
        //String resource = "getMyEarnings"; // the resource that is going to be accessed
        String action = "match"; // the operation that the user performs on the resource

        /*
         * STATES of the bot
         */
        val init = state("Init");
        val awaitingInput = state("AwaitingInput");
        val handleWelcome = state("HandleWelcome");
        val printProductInformation = state("PrintProductInformation");
        val printOrderStatus = state("PrintOrderStatus");
        val printEmployeeMonthlyGoals = state("PrintEmployeeMonthlyGoals");
        val informAboutPermissions = state("InformAboutPermissions");

        init
                .next()
                .when(eventIs(ReactEventProvider.ClientReady)).moveTo(awaitingInput);

        awaitingInput
                .next()
                //move to handledWelcome when the Greetings intent is matched && the user has permissions to reach this intent
                .when(intentIs(greetings).and(c -> enforcer.enforce(role,"greetings",action))).moveTo(handleWelcome)

                //move to printProductInformation when the getProductInformation intent is matched && the user has permission to reach this intent
                .when(intentIs(getProductInformation).and(c -> enforcer.enforce(role,"getProductInformation",action))).moveTo(printProductInformation)

                //move to printOrderStatus when the TrackMyOrder intent is matched && the user has permission to reach this intent
                .when(intentIs(trackMyOrder).and(c -> enforcer.enforce(role,"trackMyOrder",action))).moveTo(printOrderStatus)

                //move to printEmployeeMonthlyGoals when the GetEmployeeMonthlyGoals intent is matched && the user has permission to reach this intent
                .when(intentIs(getEmployeeMonthlyGoals).and(c -> enforcer.enforce(role,"getEmployeeMonthlyGoals",action))).moveTo(printEmployeeMonthlyGoals)

                //move to informAboutPermissions otherwise (when any intent is matched && the user has not permission to reach it)
                .when(intentIs(greetings).and(c -> !enforcer.enforce(role,"greetings",action)).or(intentIs(getProductInformation).and(c -> !enforcer.enforce(role,"getProductInformation",action))).or(intentIs(trackMyOrder).and(c -> !enforcer.enforce(role,"trackMyOrder",action))).or(intentIs(getEmployeeMonthlyGoals).and(c -> !enforcer.enforce(role,"getEmployeeMonthlyGoals",action)))).moveTo(informAboutPermissions);


        handleWelcome
                //This state provides different messages depending on the user role
                .body(context -> {
                    //Welcome message for free users
                    if (role == "anonymous"){
                        reactPlatform.reply(context, "Hi. Welcome to our online shop!");
                    }
                    //Welcome message for registered users
                    else if (role == "customer"){
                        reactPlatform.reply(context, "Hi customer. Welcome again to our online shop!");
                    }
                    else if (role == "employee"){
                        reactPlatform.reply(context, "Hi employee. Happy to see you!");
                    }
                })
                .next()
                .moveTo(awaitingInput);

        printProductInformation
                .body(context -> {
                    reactPlatform.reply(context, "We have the following types of " + context.getIntent().getValue("product") + "...");
                })
                .next()
                .moveTo(awaitingInput);

        printOrderStatus
                .body(context -> reactPlatform.reply(context, "The order " + context.getIntent().getValue("order") + " is in ... right now and it will arrive on ..."))
                .next()
                .moveTo(awaitingInput);

        printEmployeeMonthlyGoals
                .body(context -> reactPlatform.reply(context, "Your monthly goals have been reached by XXX%. Keep working!"))
                .next()
                .moveTo(awaitingInput);

        informAboutPermissions
                .body(context -> reactPlatform.reply(context, "You do not have permissions to see this information."))
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


