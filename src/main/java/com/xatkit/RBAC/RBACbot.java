package com.xatkit.RBAC;

import com.xatkit.core.XatkitBot;
import com.xatkit.plugins.react.platform.ReactPlatform;
import com.xatkit.plugins.react.platform.io.ReactEventProvider;
import com.xatkit.plugins.react.platform.io.ReactIntentProvider;
import lombok.val;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.casbin.jcasbin.main.Enforcer;
import static com.xatkit.dsl.DSL.*;

public class RBACbot{

    public static void main(String[] args) {

        /*
         * INTENTS definition
         */
        val findProductIntent = intent("FindProductIntent")
                .trainingSentence("I want information about PRODUCT_TYPE")
                .trainingSentence("Have you got PRODUCT_TYPE?")
                .trainingSentence("Do you have PRODUCT_TYPE?")
                .trainingSentence("I want to see PRODUCT_TYPE")
                .parameter("productType").fromFragment("PRODUCT_TYPE").entity(any());

        val getProductDetailsIntent = intent("GetProductDetailsIntent")
                .trainingSentence("Give me information about PRODUCT")
                .trainingSentence("I want more information about PRODUCT")
                .trainingSentence("I want to see PRODUCT")
                .trainingSentence("Tell me about PRODUCT")
                .parameter("product").fromFragment("PRODUCT").entity(any());

        val buyProductIntent = intent("BuyProductIntent")
                .trainingSentence("I want to buy UNITS PRODUCT")
                .parameter("units").fromFragment("UNITS").entity(number())
                .parameter("product").fromFragment("PRODUCT").entity(any());

        val updateShopCatalogueIntent = intent("UpdateShopCatalogueIntent")
                .trainingSentence("I want to update the shop catalogue")
                .trainingSentence("I want to update the catalogue")
                .trainingSentence("I want to update the price of a product");

        ReactPlatform reactPlatform = new ReactPlatform();
        ReactEventProvider reactEventProvider = new ReactEventProvider(reactPlatform);
        ReactIntentProvider reactIntentProvider = new ReactIntentProvider(reactPlatform);

        /*
         * POLICY ENFORCEMENT
         */
        Enforcer enforcer = new Enforcer("./src/RBAC/rbac_model.conf", "./src/RBAC/rbac_policy.csv");

        String testRole = "registered"; // the role of the user that wants to access a resource

        /*
         * STATES of the bot
         */
        val initState = state("InitState");
        val greetUserState = state("GreetUserState");
        val showMainMenuState = state("showMainMenuState");
        val findProductState = state("FindProductState");
        val getBasicProductDetailsState = state("GetBasicProductDetailsState");
        val getFullProductDetailsState = state("GetFullProductDetailsState");
        val buyProductState = state("buyProductState");
        val updateShopCatalogueState = state("UpdateShopCatalogueState");
        val informAboutPermissionsState = state("InformAboutPermissions");

        initState
                .next()
                //When the bot starts, the user is automatically greeted
                .when(eventIs(ReactEventProvider.ClientReady)).moveTo(greetUserState);

        greetUserState
                .body(context -> reactPlatform.reply(context, "Hi, welcome to our online shop! How can I help you?"))
                .next()
                //move to showMainMenuState when the role has permission to navigate this transition from the greetUserState
                .when(c -> enforcer.enforce(testRole,"FromGreetUserStateToShowMainMenuState","navigate")).moveTo(showMainMenuState)
                //inform about permissions otherwise
                .when(c -> !enforcer.enforce(testRole,"FromGreetUserStateToShowMainMenuState","navigate")).moveTo(informAboutPermissionsState);

        showMainMenuState
                .next()

                //move to findProductState when the FindProduct intent is matched && the role has permission to match this intent
                .when(intentIs(findProductIntent).and(c -> enforcer.enforce(testRole,"FindProductIntent","match"))).moveTo(findProductState)
                //inform about permissions otherwise
                .when(intentIs(findProductIntent).and(c -> !enforcer.enforce(testRole,"FindProductIntent","match"))).moveTo(informAboutPermissionsState)

                //move to buyProductState when the BuyProduct intent is matched && the role has permission to match this intent
                .when(intentIs(buyProductIntent).and(c -> enforcer.enforce(testRole,"BuyProductIntent","match"))).moveTo(buyProductState)
                //inform about permissions otherwise
                .when(intentIs(buyProductIntent).and(c -> !enforcer.enforce(testRole,"BuyProductIntent","match"))).moveTo(informAboutPermissionsState)

                //move to updateShopCatalogueState when the UpdateShopCatalogue intent is matched && the role has permission to match this intent
                .when(intentIs(updateShopCatalogueIntent).and(c -> enforcer.enforce(testRole,"UpdateShopCatalogueIntent","match"))).moveTo(updateShopCatalogueState)
                //inform about permissions otherwise
                .when(intentIs(updateShopCatalogueIntent).and(c -> !enforcer.enforce(testRole,"UpdateShopCatalogueIntent","match"))).moveTo(informAboutPermissionsState);

        findProductState
                .body(context -> {
                    reactPlatform.reply(context, "We have the following types of " + context.getIntent().getValue("productType") + "...");
                })
                .next()
                //move to getBasicProductDetailsState when the GetProductDetails intent is matched && the role has permission to reach the state GetBasicProductDetails
                .when(intentIs(getProductDetailsIntent).and(c -> enforcer.enforce(testRole,"GetBasicProductDetailsState","reach"))).moveTo(getBasicProductDetailsState)
                //move to getFullProductDetailsState when the GetProductDetails intent is matched && the role has permission to reach the state GetFullProductDetails
                .when(intentIs(getProductDetailsIntent).and(c -> enforcer.enforce(testRole,"GetFullProductDetailsState","reach"))).moveTo(getFullProductDetailsState)
                //move to getFullProductDetailsState otherwise
                .when(intentIs(getProductDetailsIntent).negate()).moveTo(showMainMenuState);

        getBasicProductDetailsState
                .body(context -> {
                    reactPlatform.reply(context, "Basic information about " + context.getIntent().getValue("product") + "...");
                })
                .next()
                //move automatically to the previous state
                .moveTo(findProductState);

        getFullProductDetailsState
                .body(context -> {
                    reactPlatform.reply(context, "Full information about " + context.getIntent().getValue("product") + "...");
                })
                .next()
                //move to buyProductState when the BuyProduct intent is matched && the role has permission to match this intent
                .when(intentIs(buyProductIntent).and(c -> enforcer.enforce(testRole,"BuyProductIntent","match"))).moveTo(buyProductState)
                //move to previous state otherwise
                .when(intentIs(buyProductIntent).negate()).moveTo(findProductState);

        buyProductState
                .body(context -> reactPlatform.reply(context, "Please, confirm you want to buy " + context.getIntent().getValue("units") + " of " + context.getIntent().getValue("product")))
                .next()
                //move to findProductState once the order has been processed and the role has permission to match this intent
                .when(intentIs(findProductIntent).and(c -> enforcer.enforce(testRole,"FindProductIntent","match"))).moveTo(findProductState)
                //move to showMainMenuState otherwise
                .when(intentIs(findProductIntent).negate()).moveTo(showMainMenuState);

        updateShopCatalogueState
                .body(context -> reactPlatform.reply(context, "Please, enter the ID of the product to be updated."))
                .next()
                //move automatically to the main menu
                .moveTo(showMainMenuState);

        informAboutPermissionsState
                .body(context -> reactPlatform.reply(context, "Sorry, you don't have permissions to perform this action."))
                .next()
                .moveTo(showMainMenuState);


        val defaultFallback = fallbackState()
                .body(context -> reactPlatform.reply(context, "Sorry, I didn't, get it"));

        val botModel = model()
                .usePlatform(reactPlatform)
                .listenTo(reactEventProvider)
                .listenTo(reactIntentProvider)
                .initState(initState)
                .defaultFallbackState(defaultFallback);

        Configuration botConfiguration = new BaseConfiguration();

        XatkitBot xatkitBot = new XatkitBot(botModel, botConfiguration);
        xatkitBot.run();

    }

}


