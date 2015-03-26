package org.greenfield

import org.apache.shiro.SecurityUtils
import java.math.BigDecimal;
import groovy.text.SimpleTemplateEngine
import org.springframework.web.context.request.RequestContextHolder 	   
	   
class ApplicationService {

    def grailsApplication
	
	def layout
	def header
	def footer
	
	static scope = "singleton"
	static transactional = true
	
	def homepage
	def properties

	
	def init(){
		if(!header && !footer){
			refresh()
			setProperties()
		}
	}
	
	
	def refresh(){
	
		if(!grailsApplication){
			grailsApplication = new Page().domainClass.grailsApplication
		}
		
		File layoutFile = grailsApplication.mainContext.getResource("templates/storefront/layout-wrapper.html").file
		String layoutContent = layoutFile.text
		
		
		def layout = Layout.findByName("STORE_LAYOUT").content
		layoutContent = layoutContent.replace("[[STORE_LAYOUT]]", layout)

		String[] split = layoutContent.split("\\[\\[CONTENT\\]\\]");
		
		header = split[0];
		
		//header = header.replace("[[CATALOGS]]", getCatalogsMain())
		header = header.replace("[[SEARCH_BOX]]", getSearchBox())
		header = header.replace("[[SHOPPING_CART]]", getShoppingCart())
		header = header.replace("[[ACCOUNT]]", getAccount())
		header = header.replace("[[GREETING]]", getGreeting())
		header = header.replace("[[LOGIN]]", getLogin())
		header = header.replace("[[LOGOUT]]", getLogout())
		header = header.replace("[[REGISTER]]", getRegister())
		header = header.replace("[[ORDER_HISTORY]]", getOrderHistory())
		header = header.replace("[[ADMIN_LINK]]", getAdminLink())
		
		footer = split[1];

		
		//footer = footer.replace("[[CATALOGS]]", getCatalogsMain())
		footer = footer.replace("[[SEARCH_BOX]]", getSearchBox())
		footer = footer.replace("[[SHOPPING_CART]]", getShoppingCart())
		footer = footer.replace("[[ACCOUNT]]", getAccount())
		footer = footer.replace("[[GREETING]]", getGreeting())
		footer = footer.replace("[[LOGIN]]", getLogin())
		footer = footer.replace("[[LOGOUT]]", getLogout())
		footer = footer.replace("[[REGISTER]]", getRegister())
		footer = footer.replace("[[ORDER_HISTORY]]", getOrderHistory())
		footer = footer.replace("[[ADMIN_LINK]]", getAdminLink())
		
		footer = footer.replace("[[GOOGLE_ANALYTICS]]", getGoogleAnalyticsCode())
		
		def storeName = getStoreName().replaceAll("[^\\w\\s]",""); 
		footer = footer.replace("[[SITE_NAME]]", storeName)
	}
	
	
	
	def getHeader(Catalog catalogInstance, String title){
		if(!header)refresh()
		
		def title_full = getStoreName() + " : " + title
		
		header = header.replace("[[TITLE]]", title_full)
		header = header.replace("[[META_KEYWORDS]]", getMetaKeywords())
		header = header.replace("[[META_DESCRIPTION]]", getMetaDescription())
		header = header.replace("[[CONTEXT_NAME]]", getContextName())
		header = header.replace("[[CATALOGS]]", getCatalogsByCatalog(catalogInstance))
		
		return this.header
	}
	
	
	
	def getHeader(String title){
		if(!header)refresh()
		
		def title_full = getStoreName() + " : " + title
		
		header = header.replace("[[TITLE]]", title_full)
		header = header.replace("[[META_KEYWORDS]]", getMetaKeywords())
		header = header.replace("[[META_DESCRIPTION]]", getMetaDescription())
		header = header.replace("[[CONTEXT_NAME]]", getContextName())
		header = header.replace("[[CATALOGS]]", getCatalogsMain())
		
		return this.header
	}
	
	def getHeader(){
		if(!header)refresh()
		header = header.replace("[[TITLE]]", "Greenfield")
		header = header.replace("[[CATALOGS]]", getCatalogsMain())
		return this.header
	}
	
	def getFooter(){
		if(!footer)refresh()
		footer = footer.replace("[[CATALOGS]]", getCatalogsMain())
		return this.footer
	}
	
	//TODO : uncomment products count in both methods
	def getCatalogsByCatalog(catalogInstance){
		
		if(!catalogInstance){
			return getCatalogsMain(catalogInstance)
		}
		
		def session = RequestContextHolder.currentRequestAttributes().getSession()
		session.catalogInstance = catalogInstance
		
		def template = '<li class="catalog-list-element ${activeClass}"><a href="${link}" title="${name}">${name}<span class="catalog-products-count">(${productsCount})</span></a></li>'
		
		if(catalogInstance?.toplevel && !catalogInstance?.subcatalogs){
			return getCatalogsMain(catalogInstance)
		}
		
		def catalogsString = "<div class=\"catalogs-list-container\">"
		catalogsString += getReturnLink(catalogInstance)
		catalogsString += getCatalogHeader(catalogInstance)
		
		catalogsString += "<ul class=\"subcatalog-list catalog-list\">"
		def catalogsList = getCatalogList(catalogInstance)
		if(catalogsList){
			catalogsList.each { c ->
				def productsCount = getCatalogProductsCount(c)
				//if(productsCount > 0){
					def link = "/${getContextName()}/catalog/products/${c.id}"
					def activeClass = c.id == catalogInstance.id ? "active-catalog" : ""
					def catalogData = [
						 	"link" : link, 
							"name" : c.name, 
							productsCount : productsCount, 
							activeClass : activeClass 
					]
					def engine = new SimpleTemplateEngine()
					def result = engine.createTemplate(template).make(catalogData)
					catalogsString += result.toString()
				//}
			}
		}
		catalogsString += "</ul>"
		catalogsString += "</div>"
		return catalogsString
	}
	
	
	def getReturnLink(catalogInstance){
		def template = '<span class="catalog-return-link">&#xAB;&nbsp;<a href="${link}">${name}</a></span>'
		
		def returnCatalog = getReturnCatalog(catalogInstance)

		def link = "/${getContextName()}/"
		def name = "Main Menu"
		if(returnCatalog){
			link = "/${getContextName()}/catalog/products/${returnCatalog.id}"
			name = returnCatalog.name
		}

		def catalogData = [
			"link" : link, 
			"name" : name,
		]
		def engine = new SimpleTemplateEngine()
		def result = engine.createTemplate(template).make(catalogData)
		return result.toString()
	}
	
	
	def getReturnCatalog(catalogInstance){
		def returnCatalog = null
		if(catalogInstance?.subcatalogs){
			if(catalogInstance.parentCatalog){
				returnCatalog = catalogInstance.parentCatalog
			}
		}else{
			if(catalogInstance?.parentCatalog?.parentCatalog){
				returnCatalog = catalogInstance?.parentCatalog?.parentCatalog
			}
		}
		return returnCatalog
	}
	
	
	def getCatalogHeader(catalogInstance){
		def catalogHeader = ""
		if(catalogInstance?.subcatalogs){
			catalogHeader = "<span class=\"catalog-list-header\">${catalogInstance?.name}</span>"
		}else{
			if(!catalogInstance?.toplevel){
				catalogHeader = "<span class=\"catalog-list-header\">${catalogInstance?.parentCatalog?.name}</span>"
			}
		}
		return catalogHeader
	}	
	
	
	def getCatalogList(catalogInstance){
		def catalogsList = []
		if(catalogInstance?.subcatalogs){
			catalogsList = catalogInstance.subcatalogs
		}else{
			if(!catalogInstance?.toplevel){
				catalogsList = catalogInstance?.parentCatalog?.subcatalogs
			}else{
				catalogsList = Catalog.findAllByToplevel(true)
			}
		}
		return catalogsList
	}
	
	
	def getCatalogsMain(){
		getCatalogsMain(null)
	}


	def getCatalogsMain(catalogInstance){	
	
		def template = '<li class="catalog-list-element ${activeClass}"><a href="${link}" title="${name}">${name}</a></li>'
	
		def toplevelCatalogs = Catalog.findAllByToplevel(true)
		def	catalogsString= "<div class=\"catalogs-list-container\">"
		catalogsString += "<ul class=\"main-catalogs-list\">"
		
		if(toplevelCatalogs){
			toplevelCatalogs.each { c ->
				def productsCount = getCatalogProductsCount(c)
				//if(productsCount > 0){
					def link = "/${getContextName()}/catalog/products/${c.id}"
					def activeClass = c.id == catalogInstance?.id ? "active-catalog" : ""
					def catalogData = [
						 	"link" : link, 
							"name" : c.name, 
							productsCount : productsCount, 
							activeClass : activeClass 
					]

					def engine = new SimpleTemplateEngine()
					def result = engine.createTemplate(template).make(catalogData)
					catalogsString += result.toString()
				//}
			}
		}
		catalogsString += "</ul>"
		catalogsString += "</div>"
	 	return catalogsString
	}
	
	
	def getCatalogProductsCount(catalogInstance){
		def productsCount = Product.createCriteria().count{
			and{
				eq("disabled", false)
				gt("quantity", 0)
				catalogs {
		    		idEq(catalogInstance.id)
		 		}
			}
		}
		return productsCount
	}
	
	
	def getFullBreadcrumbs(catalogInstance){
		def path = new StringBuffer()
		def link = "/${getContextName()}/catalog/products/${catalogInstance.id}"
		path.append("<a href=\"${link}\">" + catalogInstance?.name + "</a>")
		if(catalogInstance?.parentCatalog){
			path.insert(0, getFullBreadcrumbs(catalogInstance.parentCatalog) + "&nbsp;&nbsp;&#xBB;&nbsp;&nbsp;")
		}else{
			path.insert(0, "<a href=\"/${getContextName()}/\">Home</a>&nbsp;&nbsp;&#xBB;&nbsp;&nbsp;")
		}
		return path.toString()
	}
	
	
	def getBreadcrumbs(catalogInstance){
		def breadcrumbs = getFullBreadcrumbs(catalogInstance)
		return breadcrumbs
	}
	
	
	
	def getSearchBox(){
		def cartHtml = "<form action=\"/${getContextName()}/product/search\">" 
		cartHtml+= "<div id=\"searchbox\">" 
		cartHtml+= "<input type=\"text\" name=\"query\" name=\"search\"/>" 
		cartHtml+= "<input type=\"submit\" id=\"search-button\" value=\"Search\" name=\"search-button\"/>"
		cartHtml+= "</div></form>"
	}
	
	def getShoppingCart(){
		return "<a href=\"/${getContextName()}/shoppingCart\" id=\"shopping-cart\" >Shopping Cart</a>"
	}
	
	def getGreeting(){
		def subject = SecurityUtils.getSubject();

		if(subject.isAuthenticated()){
			return "<span id=\"greeting\">Welcome back <a href=\"/${getContextName()}/account/customer_profile\">${subject.principal}</a></span>"
		}else{
			return "<span></span>"
		}
	}
	
	def getLogin(){
		def subject = SecurityUtils.getSubject();
		if(!subject.isAuthenticated()){
			return "<a href=\"/${getContextName()}/auth/customer_login\" id=\"login\">Login</a>"
		}else{
			return ""
		}
	}
	
	def getLogout(){
		def subject = SecurityUtils.getSubject();
		if(subject.isAuthenticated()){
			return "<a href=\"/${getContextName()}/auth/signOut\" id=\"logout\">Logout</a>"
		}else{
			return ""
		}
		
	}
	
	def getRegister(){
		def subject = SecurityUtils.getSubject();
		if(!subject.isAuthenticated()){
			return "<a href=\"/${getContextName()}/account/customer_registration\" id=\"register\">Register</a>"
		}else{
			return "<span></span>"
		}
	}
	
	def getAccount(){
		return "<a href=\"/${getContextName()}/account/customer_profile\" id=\"my-account\">My Account</a>"
	}
	
	def getOrderHistory(){
		return "<a href=\"/${getContextName()}/account/order_history\" id=\"account-order-history\">Order History</a>"
	}
	
	def getAdminLink(){
		return "<a href=\"/${getContextName()}/admin\" id=\"admin-link\">Administration</a>"
	}
	
	
	def setProperties(){
		properties = new Properties();
		try{
			if(!grailsApplication){
				grailsApplication = new Page().domainClass.grailsApplication
			}
			
			File propertiesFile = grailsApplication.mainContext.getResource("settings/settings.properties").file
			
			FileInputStream inputStream = new FileInputStream(propertiesFile)
			if(inputStream){
				properties.load(inputStream);
			}
		} catch (IOException e){
		    log.debug"Exception occured while reading properties file :"+e
		}
	}
	
	def getContextName(){
		if(!properties)setProperties()
		String contextName = properties.getProperty("app.context");
		//println "++++++++ Store Name : ${contextName} +++++++++++"	
		return contextName
	}
	
	
	def getStoreName(){
		if(!properties){
			setProperties()
		}
		String storeName = properties.getProperty("store.name");
		//println "++++++++ Store Name : ${storeName} +++++++++++"	
		return storeName ? storeName : ""	
	}
	
	def getMetaKeywords(){
		if(!properties)setProperties()
		String keywords = properties.getProperty("meta.keywords");
		//println "++++++++ keywords : ${keywords} +++++++++++"	
		return keywords
	}
	
    def getMetaDescription() {
		if(!properties)setProperties()
		String description = properties.getProperty("meta.description");
		//println "++++++++ description : ${description} +++++++++++"	
		return description
    }
	
    def getGoogleAnalyticsCode() {
		if(!properties)setProperties()
		String googleAnalytics = properties.getProperty("google.analytics");
		//println "++++++++ googleAnalytics : ${googleAnalytics} +++++++++++"	
		return googleAnalytics
    }
	
    def getShipping() {
		if(!properties)setProperties()
		
		def shipping = properties.getProperty("store.shipping");
		
		//println "++++++++ shipping : ${shipping} +++++++++++"	
		if(!shipping){
			return 0
		}else{
			return new BigDecimal(shipping)
		}
    }
	
	
    
	def getTaxRate() {
		if(!properties)setProperties()
		String taxRate = properties.getProperty("store.tax.rate");
		//println "++++++++ taxRate : ${taxRate} -> ${new BigDecimal(taxRate)}+++++++++++"			
		if(!taxRate){
			return 0
		}else{
			return new BigDecimal(taxRate)
		}
    }
			
	def resetHomePage(){
		homepage = Page.findByTitle("Home")
	}
	
	def getHomePage(){
		if(!homepage){
			homepage = Page.findByTitle("Home")
		}
		return homepage.content
	}
	
	
	
	/******** STRIPE SETTINGS ********/

	def getStripeDevelopmentApiKey(){
		if(!properties)setProperties()
		String apiKey = properties.getProperty("stripe.development.apiKey");
		//println "++++++++ apiKey : ${apiKey} +++++++++++"	
		return apiKey
	}
	
	def getStripeDevelopmentPublishableKey(){
		if(!properties)setProperties()
		String publishableKey = properties.getProperty("stripe.development.publishableKey");
		//println "++++++++ publishableKey : ${publishableKey} +++++++++++"	
		return publishableKey
	}
	
	def getStripeLiveApiKey(){
		if(!properties)setProperties()
		String apiKey = properties.getProperty("stripe.production.apiKey");
		//println "++++++++ apiKey : ${apiKey} +++++++++++"	
		return apiKey	
	}
	
	def getStripeLivePublishableKey(){
		if(!properties)setProperties()
		String publishableKey = properties.getProperty("stripe.production.publishableKey");
		//println "++++++++ publishableKey : ${publishableKey} +++++++++++"	
		return publishableKey		
	}
	
	
	
	
	/******** MAIL SETTINGS ********/
	
	def getAdminEmailAddress(){
		if(!properties)setProperties()
		String adminEmail = properties.getProperty("mail.smtp.adminEmail");
		//println "++++++++ adminEmail : ${adminEmail} +++++++++++"	
		return adminEmail
	}
	
	def getSupportEmailAddress(){
		if(!properties)setProperties()
		String supportEmail = properties.getProperty("mail.smtp.supportEmail");
		//println "++++++++ supportEmail : ${supportEmail} +++++++++++"	
		return supportEmail
	}
		
	def getMailUsername(){
		if(!properties)setProperties()
		String username = properties.getProperty("mail.smtp.username");
		//println "++++++++ apiKey : ${username} +++++++++++"	
		return username
	}
	
	def getMailPassword(){
		if(!properties)setProperties()
		String password = properties.getProperty("mail.smtp.password");
		//println "++++++++ password : ${password} +++++++++++"	
		return password
	}
		
	def getMailAuth(){
		if(!properties)setProperties()
		String auth = properties.getProperty("mail.smtp.auth");
		//println "++++++++ auth : ${auth} +++++++++++"	
		return auth
	}
	
	def getMailStartTlsEnabled(){
		if(!properties)setProperties()
		String startTlsEnabled = properties.getProperty("mail.smtp.starttls.enabled");
		//println "++++++++ startTlsEnabled : ${startTlsEnabled} +++++++++++"	
		return startTlsEnabled
	}
	
	def getMailHost(){
		if(!properties)setProperties()
		String host = properties.getProperty("mail.smtp.host");
		//println "++++++++ host : ${host} +++++++++++"	
		return host	
	}
	
	def getMailPort(){
		if(!properties)setProperties()
		String port = properties.getProperty("mail.smtp.port");
		//println "++++++++ port : ${port} +++++++++++"	
		return port		
	}
	
	
	
	/******** SHIPPING SETTINGS ********/
	
	def getStoreAddress1(){
		if(!properties)setProperties()
		String address1 = properties.getProperty("store.address1");
		//println "++++++++ address1 : ${address1} +++++++++++"	
		return address1
	}
	
	def getStoreAddress2(){
		if(!properties)setProperties()
		String address2 = properties.getProperty("store.address2");
		//println "++++++++ address2 : ${address2} +++++++++++"	
		return address2
	}
	
	def getStoreCity(){
		if(!properties)setProperties()
		String city = properties.getProperty("store.city");
		//println "++++++++ city : ${city} +++++++++++"	
		return city
	}
	
	def getStoreState(){
		if(!properties)setProperties()
		String state = properties.getProperty("store.state");
		//println "++++++++ state : ${state} +++++++++++"	
		return state
	}
	
	def getStoreZip(){
		if(!properties)setProperties()
		String zip = properties.getProperty("store.zip");
		//println "++++++++ zip : ${zip} +++++++++++"	
		return zip
	}
	
	
	def getEasyPostEnabled(){
		if(!properties)setProperties()
		String enabled = properties.getProperty("easypost.enabled");
		//println "++++++++ enabled : ${enabled} +++++++++++"	
		return enabled		
	}
	
	def getEasyPostTestApiKey(){
		if(!properties)setProperties()
		String apiKey = properties.getProperty("easypost.test.apiKey");
		//println "++++++++ apiKey : ${apiKey} +++++++++++"	
		return apiKey		
	}
	
	def getEasyPostLiveApiKey(){
		if(!properties)setProperties()
		String apiKey = properties.getProperty("easypost.live.apiKey");
		//println "++++++++ apiKey : ${apiKey} +++++++++++"	
		return apiKey		
	}
	
	
	
	
	def formatPrice(price){
		if(price){
 	   		BigDecimal numberBigDecimal = new BigDecimal(price);
			numberBigDecimal  = numberBigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP);
			
			return numberBigDecimal
		}else{
			return 0
		}
	}
	
}
