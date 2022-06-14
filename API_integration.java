@Stateless
@Local(WsServiceManagerRedbus.class)
public class RedBusWsServiceManager implements WsServiceManagerRedbus {

	/* -------------------------------------------------- */
	
	/*
	 * 		ANTES DEL CÓDIGO
	 * 
	 * 		El service manager de Redbus implementa con código propio solo 10 de los 22 métodos que debe incorporar
	 * por implementar la interfaz WsServiceManagerRedBus que extiende de WsServiceManager.
	 * De esos 10 métodos, solo 7 están desarrollados en función de los requerimientos de la API de Redbus.
	 * Los 3 métodos restantes fueron implementados con código propio solamente para cumplir con los requerimientos
	 * del proceso de nuestro sistema (TOL). Hablamos, concretamente, de los métodos:
	 * 
	 * 		a) bloquearButaca(ServicioWrap, String, CredencialWrap) : BloqueoButaca
	 * 		b) generarNroConexion(InstanciaWS, CredencialWrap): int
	 * 		c) generarNroConexion(InstanciaWS, CredencialWrap): int
	 * 
	 * 		La documentación que da cuenta del por qué de la implementación de estos tres métodos se podrá encontrar más adelante,
	 * justo antes del desarrollo de cada uno de ellos.
	 * 
	 * 		Antes de tomar esta integración como referencia para otras integraciones, es recomendable tener los TRES puntos que se
	 * aclararán a continuación en cuenta:
	 * 
	 * 			1-. Al momento de llevarse a cabo la integración (en su primera versión, mayo 2022) del servicio Redbus TOL utilizaba
	 * 		los siguentes endpoints (apuntando a los métodos declarados en la clase ServiciosRest):
	 * 
	 * 		I	GET 	/TOLWeb/api/rest/v1.0/authorize*
	 * 		II	POST	/TOLWeb/api/rest/v1.0/trips
	 * 		III	GET	/TOLWeb/api/rest/v1.0/seats/{{tripKey}}/{{tripKeyVuelta}}
	 * 		IV	POST	/TOLWeb/api/rest/v1.0/multi/checkout
	 * 		V	GET	/TOLWeb/api/rest/v1.0/payments/options*
	 * 	 	VI	POST	/TOLWeb/api/rest/v1.0/payments/init*
	 * 		VII 	POST	/TOLWeb/api/rest/v1.0/multi/book
	 * 		VIII	POST	/TOLWeb/api/rest/v1.0/transaction/{{localizador}}/cancel
	 * 		VIII'	POST	/TOLWeb/api/rest/v1.0/transaction/{{localizador}}/refund
	 * 
	 * 		(*: No requieren desarrollo específico para el servicio a integrar)
	 * 
	 * 		Cada uno de estos procesos está señalado en este archivo con su respectivo título precedido por la marca "{!}".
	 * 
	 * 			2-. En lo que respecta a la API de Redbus, para la misma fecha (mayo 2022) el proceso de compra y cancelación de 
	 * 		pasajes se correspondía con los que pueden observarse en la colección de Postman compartida en este respositorio
	 * 		de github: https://github.com/idevillafane/rebus_api_postman.git
	 * 
	 * 			3-. En relación a asuntos que que competen exclusivamente al modo en que se desarrolló el código de TOL desde sus inicios
	 * 		hasta el día de la fecha, vale tener en cuenta que inicialmente el sistema no estaba preparado para procesar el envío de datos
	 * 		de pasajeros (representados en TOL mediante la clase Persona y relacionados al DTO Person) a la API del servicio a integrar
	 * 		antes de la venta/book de los pasajes (hablamos de nombre, apellido, documento, etc.). Pero Redbus (al igual que otros servicios
	 * 		como Imakio, Flixbus, Pasajebus o Turnit) exigen que tal información se envíe al momento de la reserva/bloq/checkout del pasaje.
	 * 
	 * 				El problema que se presentaba entonces era que el sistema estaba diseñado para procesar menos información antes de llegar
	 * 		a los métodos relacionados con el proceso de CHECKOUT, los cuales son llamados en el siguiente orden por ServiceLocator y son:
	 * 		
	 * 			a) bloquearButaca(ServicioWrap, String, CredencialWrap) : BloqueoButaca (sin desarrollo propio en Redbus)
	 * 			b) getTarifas(ServicioWrap, String, CredencialWrap) : DataTarifas (donde se procesa la reserva)
	 * 		
	 * 				En ambos métodos, el parámetro String recibe el nombre de 'butacas' y es a través del cual se decidió enviar los datos
	 * 		relacionados con los pasajeros con código especialmente desarrollado para crear cadenas a medida que la información pasaba
	 * 		desde RestSession a TaquillaControllerAction y más adelante a TxUpdaterBean.
	 * 
	 * 				A partir de la integración de Redbus, se implementó la clase Butaca, que integra los datos de las clases Taquilla y Persona
	 * 		(más otra clase con datos específicos para la venta del pasaje) que antes se enviaban como un String semi-harcodeado a través
	 * 		del mencionado String butacas y ahora los entrega como la resresentación de los objetos en formato JSON.
	 * 
	 * 				En conclusión -para resumir lo mencionado en este tercer punto-, antes de tomar este archivo comor referencia para desarrollar
	 * 		la integración de otro servicio es recomendable analizar si este nuevo servicio requiere de la implementación de la clase Butaca o mas
	 * 		bien procesa la información de los pasajeros tal como se hacía en TOL originalmente. En cualquier caso, y para finalizar este tema en
	 * 		particular, tengase en cuenta que si lo que se desea es explorar el funcionamiento de la clase Butaca, se aconseja inspeccionar los 
	 * 		métodos de ButacaUtils.
	 *		
	 *		REFERENCIAS
	 *
	 *		Para mayor claridad, se utilizará el siguiente sistema de signos para indicar las diversas razones por las cuales se pudo haber escrito 
	 *		cada segmento de código:
	 *		
	 *			{!} :	Etapa del proceso de venta (endpoint de TOL)
	 *			{^} :	Implementación de métodos (o declaración de variables) requeridos por TOL (para serialización u otros motivos) cuyo desarrollo
	 *				no varía (o puede llevarse a cabo de igual manera) entre los distintos service manager.
	 *			{#} :	Código cuya lógica puede variar sustancialmente entre un ServiceManager y otro debido a que depende fuertemente de la lógica
	 *				de negocio propia del servicio a integrar.
	 *			{@} :	Métodos, lógica, librerías o recursos que comenzaron a implementarse con Redbus y que antes se desarrollaban en TOL de otra manera.
	 *			{n} :	Donde n es el número de orden del paso dentro de la estructura general del metodo referenciado. Ver "ESTRUCTURA GENERAL DE LOS
	 *				MÉTODOS IMPLEMENTADOS" más adelante.
	 *
	 *		ESTRUCTURA GENERAL DE LOS MÉTODOS
	 *
	 *			A continuación se brindarán detalles elementales sobre el funcionamiento TOL en su vínculo con la API de Redbus. Además se hará mención
	 *		a utilidades básicas de Eclipse IDE que resultarán de ayuda tanto para las tareas de desarrollo como de debugging. Los programadores con
	 *		cierta experiencia pueden obviar este bloque.
	 *		
	 *			Tal como se puede observar en el repositorio ya mencionado (https://github.com/idevillafane/rebus_api_postman.git), la API de Redbus
	 *		utiliza 5 endpoints para completar el proceso total de venta y cancelación de pasajes. Se puede agregar un sexto endpoint si se contempla
	 *		también la cancelación de reservas de pasajes. A saber, tales endpoints son:
	 *			
	 *				a)	GET	/availabletrips 	(Trae información sobre todos los viajes disponibles)
	 *				b)	GET	/tripdetails		(Trae información sobre las butacas de un viaje específico)
	 *				c)	POST	/blockticket		(Reserva el boleto por un tiempo determinado)
	 *				d)	GET	/bookandgetticket 	(Compra el pasaje y trae información sobre el mismo)
	 *				e)	POST	/cancelticket		(Cancela el pasaje comprado)
	 *		
	 *			Si bien cada servicio posee lógica distinta (otras API's de otros servicios pueden tener más o menos endpoints, y más o menos requerimientos
	 *		de información) es bueno entender cómo funciona esta integración en particular a fin de poder determinar con mejor criterio qué parte del código puede
	 *		ser copiado y pegado sin mayores cambios en otra integración, qué parte del codigo sirve a medias y a qué parte del código es mejor no prestarle atención.
	 *		Precisamente, lo que se busca lograr con las anotaciones "{#}" y "{@}" es facilitar la comprensión general del negocio y brindar herramientas para que
	 *		los desarrolladores que se encuentran por primera vez con TOL puedan entender los distintos cambios realizados sobre el sistema y difenrenciar aquellos
	 *		que se implementaron para mejorar su rendimiento de aquellos otros derivados de la intervención de distintos programadores a lo largo del tiempo, con
	 *		diferentes formas y estilos de agregar sus líneas de código.
	 *
	 *			Así, se recomienda tomar aquellos comentarios con la referencia "{#}" como lo que son: advertencias de que el código comentado en ese punto puede no
	 *		funcionar para todos los servicios. Del mismo modo, se alienta a tomar aquellos comentarios con la referencia "{@}" como sugerencias para utilizar el
	 *		el código comentado en ese punto en futuras implementaciones.
	 *
	 *		ESTRUCTURA GENERAL DE LOS MÉTODOS IMPLEMENTADOS
	 *	
	 *			En términos generales, podemos decir que cada método implementado se corresponde a una llamada a la API del servicio integrado. De todos modos, es
	 *		importante enfatizar que esto es así solo en términos generales. En la práctica (y el caso de Redbus no es la excepción) hay métodos cuya implementación
	 *		es virtualmente innecesaria (como con esos 12 métodos restantes de los 22 mencionados al inicio de este documento), otros que se implementan unicamente
	 *		para cumplir con los requisitos del flujo de tráfico de datos que TOL impone (como con aquellos 3 puntualizados también al principio de este documento) y
	 *		otros que llaman a más de un endpoint de la API que provee el servicio (en Redbus no hay ningun caso).
	 *
	 *			Con respecto a las distintas estructuras que podemos encontrar, en esta integración en particular hallaremos DOS (una para los métodos http de tipo GET
	 *		y otra parapara los métodos http de tipo POST), de las cuales una, a su vez, tendra DOS variantes (una con lógica específica para porcesar Ida y Vuelta por
	 *		por separado y otra, más simple, con la lógica de Ida y Vuelta oculta).
	 *		
	 *			ESTRUCTURA DE LOS MÉTODOS GET
	 *
	 *			+ declaraciónDelMétodo() {
	 *		
	 *				{1.a}	Seteo tipo de evento logger
	 *				{4.a}	Instanciación de variables necesarias para llamar a endpoint
	 *				{1.b}	Seteo de los parametros de logger
	 *				{4.b}	Declaración de la String con la url del enpoint
	 *				try {
	 *				{4.c}	Llamado a endpoint
	 *						if status == 200
	 *					{5.a}		Captura de la respuesta
	 *					{5.b}		transformación de la respuesta en objeto propio mediante gson
	 *					{1.c}		seteo de mensaje de logger
	 *					{6.a}		LÓGICA DE CONTROLADOR PARA ARMAR RESPUESTA REQUERIDA POR EL MÉTODO (¡LO MÁS IMPORTANTE!)
	 *						else status != 200
	 *				} catch excepciones {}
	 *				{1.d}	saveRegistroLog( new LogWs())
	 *			{6.b}	return
	 *			}
	 *
	 *			ESTRUCTURA DE LOS MÉTODOS POST*
	 *
	 *			+ declaraciónDelMétodo() {
	 *		
	 *				{1.a}	Seteo tipo de evento logger
	 *				{2.a}	Instanciación de objeto para armar json request body
	 *				{2.b}	LÓGICA DE ARMADO DE REQUEST BODY
	 *				{2.c}	Creación de JSON mediante Gson
	 *				{4.a}	Instanciación de variables necesarias para llamar a endpoint
	 *				{1.b}	Seteo de los parametros de logger
	 *				{4.b}	Declaración de la String con la url del enpoint
	 *				try {
	 *				{4.c}	Llamado a endpoint
	 *						if status == 200
	 *					{5.a}		Captura de la respuesta
	 *					{5.b}		transformación de la respuesta en objeto propio mediante gson
	 *					{1.c}		seteo de mensaje de logger
	 *					{6.a}		LÓGICA DE CONTROLADOR PARA ARMAR RESPUESTA REQUERIDA POR EL MÉTODO (¡LO MÁS IMPORTANTE!)
	 *						else status != 200
	 *				} catch excepciones {}
	 *				{1.d}	saveRegistroLog( new LogWs())
	 *			{6.b}	return
	 *			}
	 *
	 *			(*: Las variantes con ida y vuelta explicita u oculta tienen estructuras similares, solo que en el primer caso se deben hacer dos llamados
	 *			desde el endpoint: uno para la ida y otro para la vuelta; ver un ejemplo de esto en "vender()". Sobre asuntos relacionados con los viajes
	 *			de Ida y Vuelta nos explayaremos a continuación. Se reserva para indicar estos pasos la referencia "{3}").
	 *
	 *			
	 */
	
	/* -------------------------------------------------- */
	
	/*
	 *  {^}	Anotaciones y parametros requeridos por Serializable e Hibernate.
	 *	Parámetros auxiliares globales para usar SimpleDateFormat, Gson y Logger
	 *	Método getType que devuelve el tipo con el que se referencia esta empresa.
	 */
	
	private static final long serialVersionUID = 1L;

	@In(create = true, required = false)
	private WsLogger wsLogger;

	@In(create = true, required = false)
	private IWsDaoManager wsDaoManager;

	@In(create = true, required = false)
	private URLEncoder urlEnconder;

	@In(create = true, required = false)
	private EmpresaDao empresaDao;

	@In(create = true, required = false)
	private PersonaDao personaDao;

	SimpleDateFormat sdfRedbus = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	SimpleDateFormat sdfTol = new SimpleDateFormat("dd/MM/yyyy HH:mm");

	Gson gson = new GsonBuilder().setDateFormat("dd/MM/yyyy").setPrettyPrinting().create();

	String loggerEvent, loggerMessage, loggerParams;

	public WsType getType() {
		return WsType.RedBus;
	}
	
	
	/* -------------------------------------------------- */

	// {!} TRIPS

	public List<ServicioWrap> getServicios(IParada origen, IParada destino, Date fecha, String conexion, String url,
			CredencialWrap credencial, String pasajeros) {

		/*{1.a}*/ TipoEventoLog loggerEvent = TipoEventoLog.servicios;

		/*{4.a}*/ List<ServicioWrap> servicios = new ArrayList<ServicioWrap>();
		/*{4.a}*/ String source = origen.getCodigo();
		/*{4.a}*/ String destination = destino.getCodigo();
		/*{4.a}*/ String doj = sdfRedbus.format(fecha);
		/*{4.a}*/ String domain = credencial.getUrl();

		/*{1.b}*/ loggerParams = "Origen: " + source + ", destino: " + destination + ", fecha: " + doj;

		/*{4.b}*/ String request = domain + "availabletrips?source=" + source + "&destination=" + destination + "&doj=" + doj;

		try {
			// {#} JerseyClient_byOauth es una clase estatica creada para realizar las peticiones HTTP mediante la libreria Jersey, autorizandola por OAuth
		/*{4.c}*/ ClientResponse clientResponse = JerseyClient_byOAuth.getJerseyWebResourceByOAuth(request);

			if (clientResponse.getStatus() == HttpStatus.SC_OK) {

				/*{5.a}*/ String response = clientResponse.getEntity(String.class);

				/*{5.b}*/ AvailableTrips availableTrips = gson.fromJson(response, AvailableTrips.class);

				/*{1.c}*/ loggerMessage = (availableTrips != null) ? "OK" : "NULL RESPONSE. HTTP STATUS: " + clientResponse.getStatus();

		/*{6.a}*/ // La lógica comienza acá =>
				
				// {@} PaxToPassangers convierte el "pax", que es un string con formato "int|int", donde se indica número de adultos y número de niños que viajan, en los int independientes correspondientes.
				PaxToPassengers pax = new PaxToPassengers(pasajeros);
				// {^} Se busca la empresa en la base de datos	
				Empresa empresa = empresaDao.findByCodigo("REDBUS");
				// Acá hay 4 (1+2+1) bucles for encadenados.
				// El 1º reccorre cada viaje mostrado por Redbus
				// Los 2 siguentes recorren, en cada viaje, cada punto de partida y cada punto de llegada, ya que Redbus puede tener más de un punto de embarque y/o desembarque por viaje.
				// El 4º 'for i' es para recorrer cada una de las tarifas del viaje y asociarla a una calidad de butaca, ya que Redbus puede entregar más de una tarifa por viaje.
				// En resumen, los primeros 3 'for i' son para desagregar los servicios con paradas multiples y 'convertirlos' en servicios con puntos de subida y bajada únicos. El 4º 'for i' es solamente para definir la calidad del asiento.
				int servicioId = 0;
				for (Trip trip : availableTrips.getAvailableTrips()) {

					if (Integer.valueOf(pax.getTotalPassengers()) > trip.getAvailableSeats())
						continue;

					Date time = DateOperations.parse(sdfRedbus, trip.getDoj());
					Date departureTime = DateOperations.add(time, trip.getDepartureTime(), Calendar.MINUTE);
					Date arrivalTime = DateOperations.add(time, trip.getArrivalTime(), Calendar.MINUTE);

					for (BoardingTime bt : trip.getBoardingTimes()) {

						for (BoardingTime dt : trip.getDroppingTimes()) {

							ServicioWrap servicioWrap = new ServicioWrap(origen, destino, empresa);
							// Como estamos creando nuevos servicios, debemos crear nuevos ids unicos para cada servicio. Esto lo logramos armando una nueva cadena con el id del servicio y los de los puntos de subida y bajada que lo integraran. 
							String servicioCodigo = RedbusUtils.buildServicioCodigo(trip, bt, dt);

							int fareCount = 0;
							
							for (BigDecimal fare : trip.getFares()) {

								String calidad = trip.isSeater() ? (trip.isSleeper() ? "SC" : "CO")
										: (!trip.isSleeper() ? "EJ" : "CS");
								servicioWrap.addTarifa(new Tarifa(fareCount == 0 ? calidad : calidad + fareCount, fare,
										trip.getAvailableSeats(), trip.getCurrencyType()));
								fareCount++;
							}
							// El servicioId es de uso interno. El verdadero id está en servicioCodigo.
							servicioWrap.setServicioId(servicioId);
							servicioWrap.setTimestamp(fecha.getTime());
							servicioWrap.setConTaquilla(!trip.getSeatLayoutDisabled());
							servicioWrap.setServicioCodigo(servicioCodigo);
							servicioWrap.setFechaOrigen(departureTime);
							servicioWrap.setFechaDestino(arrivalTime);
							servicioWrap.setButacasSeleccionadas(pax.getTotalPassengers());
							servicioWrap.setHoraOrigen(sdfTol.format(departureTime)); // sw.setTzOrigen(departureTz);
							servicioWrap.setHoraDestino(sdfTol.format(arrivalTime)); // sw.setTzDestino(arrivalTz);
							servicioWrap.setAceptaDevolucion(trip.getPartialCancellationAllowed());
							// Los siguientes parametros no son requeridos para el funcionamiento de TOL. Simplemente se los completó para contar con mayor información.
							servicioWrap.setTourDescripcion(trip.getVehicleType());
							servicioWrap.setInfoTour(trip.getBusType());
							
							servicios.add(servicioWrap);
							servicioId++;
							
		/*{6.a}*/ // <= La lógica termina acá.					
						}
					}
				}
			// En general, Redbus solo devuelve codigos 200 y 500 (o 400 si hay problemas con OAUTH, pero eso ya esta manejado por otro lado), por lo que no hace falta explicitar más códigos en estos 'else if'.	
			} else if (clientResponse.getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
				loggerMessage = "ERROR: " + clientResponse.getStatus();
			} else {
				loggerMessage = "HTTP STATUS: " + clientResponse.getStatus();
			}

		} catch (ClientHandlerException e) {
			e.printStackTrace();
			loggerMessage = e.toString();
		} catch (Exception e) {
			e.printStackTrace();
			loggerMessage = e.toString();
		}

	/*{1.d}*/	wsLogger.saveRegistroLog(new LogWs(credencial, loggerEvent, loggerMessage, loggerParams));

/*{6.b}*/	return servicios;
	}

	/* -------------------------------------------------- */

	// {!} SEATS

	public DataTaquilla getTaquilla(ServicioWrap servicio, CredencialWrap credencial) {

		TipoEventoLog loggerEvent = TipoEventoLog.taquilla;

		DataTaquilla dataTaquilla = new DataTaquilla();
		List<Taquilla> listTaquilla = new ArrayList<Taquilla>();
		// 🍬 REQUEST DATA STRINGS
		String domain = credencial.getUrl();
		String tripId = RedbusUtils.buildTripId(servicio);

		loggerParams = "Trip Id: " + tripId;

		String request = domain + "tripdetails?id=" + tripId;

		try {
			ClientResponse clientResponse = JerseyClient_byOAuth.
					getJerseyWebResourceByOAuth(request);

			if (clientResponse.getStatus() == HttpStatus.SC_OK) {

				String response = clientResponse.getEntity(String.class);

					TripDetails tripDetails = gson.fromJson(response, TripDetails.class);
					
					loggerMessage = (tripDetails != null) ? "OK" : "NULL RESPONSE. HTTP STATUS: " + clientResponse.getStatus();

					for (Seat seat : tripDetails.getSeats()) {

						Taquilla taquilla = new Taquilla(servicio);

						/*
						 * Una taquilla es un asiento. Acá se deben completar los datos necesarios para
						 * poder renderizar y vender asientos específicos. Hay casos en los que este
						 * paso se ignora porque el servicio que nos provee la información no entrega o
						 * no requiere que se renderice un mapa de asientos.
						 */
						taquilla.setTipo(TaquillaType.Butaca); // ES IMPORTANTE QUE DIGA "BUTACA" -
																// TaquillaUtils.build2()
						taquilla.setPrecio(seat.getFare().doubleValue());
						taquilla.buildFilaColumna(seat.getColumn() + 1, seat.getRow() + 1);
						taquilla.setPiso(seat.getRow() < (5 + 1) ? TaquillaFloor.Abajo_B : TaquillaFloor.Arriba_A); // LÓGICA
																													// REDBUS
						taquilla.setCalidad("CO");
						taquilla.setMoneda(tripDetails.getCurrencyType());
						taquilla.setTexto(seat.getName()); // "TEXTO" ES EL TEXTO QUE LLEVA EL ASIENTO EN EL MAPA
															// GRAFICADO
						taquilla.setEstado(seat.getAvailable() ? TaquillaEstado.FREE : TaquillaEstado.NOT_FREE);
						/*
						 * Los siguientes parametros no son requeridos para el funcionamiento de TOL.
						 * Simplemente se los completó para contar con mayor información.
						 */
						taquilla.setColor(seat.getLadiesSeat() ? "Solo para mujeres" : "Asiento sin genero");
						taquilla.setColorCalidad(seat.getSeatType());

						listTaquilla.add(taquilla);
					}
					
					dataTaquilla.setTaquillas(listTaquilla);
			} else if (clientResponse.getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
				loggerMessage = "ERROR: " + clientResponse.getStatus();
			} else {
				loggerMessage = "HTTP STATUS: " + clientResponse.getStatus();
			}
		} catch (ClientHandlerException e) {
			e.printStackTrace();
			loggerMessage = e.toString();
		} catch (Exception e) {
			e.printStackTrace();
			loggerMessage = e.toString();
		}

		wsLogger.saveRegistroLog(new LogWs(credencial, loggerEvent, loggerMessage, loggerParams));

		return dataTaquilla;
	}

	/* -------------------------------------------------- */

	// {!} CHECKOUT -> Reservar

	public BloqueoButaca bloquearButaca(ServicioWrap servicio, String butacas, CredencialWrap credencial) {

		TipoEventoLog loggerEvent = TipoEventoLog.bloquearButaca;
		loggerParams = "-";
		loggerMessage = "EL BLOQUEO SE REALIZA EN GET TARIFAS";
		wsLogger.saveRegistroLog(new LogWs(credencial, loggerEvent, loggerMessage, loggerParams));
		return new BloqueoButaca(ResultadoBloqueoButaca.EL_BLOQUEO_SE_REALIZA_EN_GET_TARIFAS);
	}

	/* -------------------------------------------------- */

	public DataTarifas getTarifas(ServicioWrap servicio, String butacas, CredencialWrap credencial) {

		TipoEventoLog loggerEvent = TipoEventoLog.tarifas;

		RedbusUtils codigos = new RedbusUtils(servicio);

		ListaButacas listaButacas = new ListaButacas();
		listaButacas = ButacaUtils.stringTaquillasToListaButacas(butacas);

		BlockTicketRequest blockTicketRequest = new BlockTicketRequest();
		ArrayList<InventoryItems> listInventoryItems = new ArrayList<InventoryItems>();

		blockTicketRequest.setAvailableTripId(new BigInteger(codigos.getTripId()));
		
		int cantidadDePasajeros = 0;

		for (Butaca butaca : listaButacas.getButacas()) {

			InventoryItems inventoryItems = new InventoryItems();

			inventoryItems.setSeatName(butaca.getTaquilla().getTexto());
			inventoryItems.setFare(butaca.getTaquilla().getPrecio());
			inventoryItems.buildName(butaca.getPerson().getName() + " " + butaca.getPerson().getLastname());
			inventoryItems.buildEmail(butaca.getPerson().getEmail());
			inventoryItems.buildAge(String.valueOf(DateOperations.ageOfPerson(butaca.getPerson().getBirthDate())));
			inventoryItems.buildGender(butaca.getPerson().getSex().equals("F") ? "FEMALE" : "MALE");
			inventoryItems.buildPrimary(butaca.getPerson().getOrder() == 1);
			inventoryItems.buildNationality(butaca.getPerson().getNationality());
			inventoryItems.buildMobile(butaca.getPerson().getPhone().equals("") ? "3416941964" : butaca.getPerson().getPhone());
			inventoryItems.buildIdType(butaca.getPerson().getIdType());
			inventoryItems.buildIdNumber(butaca.getPerson().getIdNumber());
			inventoryItems.buildAddress("Segurola y Habana 4310");

			cantidadDePasajeros++;

			listInventoryItems.add(inventoryItems);
		}

		blockTicketRequest.setInventoryItems(listInventoryItems);
		blockTicketRequest.setBoardingPointId(Integer.valueOf(codigos.getBoardingPointId()));
		blockTicketRequest.setDroppingPointId(Integer.valueOf(codigos.getDroppingPointId()));
		blockTicketRequest.setSource(Integer.valueOf(codigos.getSource()));
		blockTicketRequest.setDestination(Integer.valueOf(codigos.getDestination()));
		blockTicketRequest.buildAdultSeatCount(cantidadDePasajeros);
		blockTicketRequest.buildChildSeatCount(0);
		blockTicketRequest.setBookingType("STANDARD");
		blockTicketRequest.setCurrencyType("ARS");

		String postString = gson.toJson(blockTicketRequest, BlockTicketRequest.class);

		loggerParams = "Request Body: " + postString;

		DataTarifas data = new DataTarifas();
		String domain = credencial.getUrl();
		String request = domain + "blockTicket";

		try {
			ClientResponse clientResponse = JerseyClient_byOAuth.postJerseyWebResourceByOAuth(request, postString);
			String response = clientResponse.getEntity(String.class);
			if (clientResponse.getStatus() == HttpStatus.SC_OK) {

				BlockResponse blockResponse = gson.fromJson(response, BlockResponse.class);

				loggerMessage = blockResponse != null ? "OK"
						: "NULL RESPONSE. HTTP STATUS: " + clientResponse.getStatus();
				/*
				 * REDBUS REQUIERE EL STRING TOTAL_FARE COMO PARAMETRO PARA CONCRETAR LA VENTA.
				 * SIN EMBARGO, ESTE STRING NO SIEMPRE COINCIDE CON EL IMPORTE DE LA SUMA DE LAS
				 * TARIFAS (DOUBLES) INDIVIDUALES. (POR EJ., HAY CASOS EN LOS QUE LAS TARIFAS
				 * SALEN $1.60 Y EL TOTAL_FARE ES "3.19") POR ESO CORRESPONDE SETEARLO COMO
				 * STRING JUNTO AL BLOCKKEY DENTRO DEL ID_CIERRE_VENTA.
				 */
				servicio.setIdCierreVenta(RedbusUtils.buildIdCierreVenta(blockResponse));
				data.setIdCierreVenta(RedbusUtils.buildIdCierreVenta(blockResponse));
				data.setTarifas(servicio.getTarifas());
			} else if (clientResponse.getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
				loggerMessage = "ERROR: " + clientResponse.getStatus();
				data = null;
			} else {
				loggerMessage = "HTTP STATUS: " + clientResponse.getStatus();
			}

		} catch (ClientHandlerException e) {
			e.printStackTrace();
			loggerMessage = e.toString();
		} catch (Exception e) {
			e.printStackTrace();
			loggerMessage = e.toString();
		}

		wsLogger.saveRegistroLog(new LogWs(credencial, loggerEvent, loggerMessage, loggerParams));
		return data;
	}

	/* -------------------------------------------------- */

	// {!} BOOK -> Vender

	public ResultadoVenta vender(DataFinalizarVentaDto data) throws WebServiceException {

		final int IDA = 1;
		final int VUELTA = 2;
		TipoEventoLog loggerEvent = TipoEventoLog.vender;

		ResultadoVenta resultadoVenta = new ResultadoVenta();
		final int CANTIDAD_DE_TRAMOS = data.getEmpresaVuelta() != null ? VUELTA : IDA;
		final BigInteger NUMBER_UTIL = new BigInteger("1000000");
		
		for (int tramo = IDA; tramo <= CANTIDAD_DE_TRAMOS; tramo++) {
			
			ListaButacas listaButacas = new ListaButacas();
			listaButacas = ButacaUtils.stringButacasToListaButacas(tramo == IDA ? data.getStringButacas() : data.getStringButacasVuelta());				
			RedbusUtils codigos = new RedbusUtils(tramo == IDA ? data.getIdCierreVenta() : data.getIdCierreVentaVuelta());
			
			String domain = data.getCredencial().getUrl();
			String blockKey = codigos.getBlockKey();
			String totalFare = codigos.getTotalFare();
			String request = domain + "bookandgetticket?blockKey=" + blockKey + "&totalFare=" + totalFare;
	
			loggerParams += (tramo == IDA ? "IDA:" : " VUELTA:") + " Block key: " + blockKey + ", total fare: " + totalFare;
	
			try {
	
				ClientResponse clientResponse = JerseyClient_byOAuth.getJerseyWebResourceByOAuth(request);
	
				if (clientResponse.getStatus() == HttpStatus.SC_OK) {
	
					String response = clientResponse.getEntity(String.class);
	
					Ticket ticket = gson.fromJson(response, Ticket.class);
	
					loggerMessage += (tramo == IDA ? "IDA: " : " VUELTA: ") + ((ticket != null) ? "OK" : "NULL RESPONSE. HTTP STATUS: " + clientResponse.getStatus());
						
					if (tramo == IDA)
						resultadoVenta.setIdTxIda(ticket.getTin());
					if (tramo == VUELTA)
						resultadoVenta.setIdTxVuelta(ticket.getTin());
					
					int butacaCounter = 0;
					for (InventoryItem inventoryItem : ticket.getInventoryItems()) {
						
						ResultadoBoleto resultadoBoleto = new ResultadoBoleto();
	
						resultadoBoleto.setNroBoleto(RedbusUtils.buildNroBoleto(ticket, inventoryItem));
						resultadoBoleto.setButaca(inventoryItem.getSeatName());
						resultadoBoleto.setOrigen(tramo == IDA ? data.getOrigen().getDescripcion() :data.getOrigenVuelta().getDescripcion());
						resultadoBoleto.setDestino(tramo == IDA ? data.getDestino().getDescripcion() : data.getDestinoVuelta().getDescripcion());
						resultadoBoleto.setImporte(inventoryItem.getFare().doubleValue());
						resultadoBoleto.setIdVenta(ticket.getInventoryId().remainder(NUMBER_UTIL).toString());
						resultadoBoleto.setFicha(ticket.getPnr());
						resultadoBoleto.setIda(tramo == IDA);
						resultadoBoleto.setTransaccion(ticket.getTin());
						resultadoBoleto.setClase(listaButacas.getButacas().get(butacaCounter).getDataButacaVenta().getCalidad());
						resultadoVenta.addBoleto(resultadoBoleto);
						butacaCounter++;
					}
				} else if (clientResponse.getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
					loggerMessage = "ERROR: " + clientResponse.getStatus();
					System.out.println(clientResponse.toString());
				} else {
					loggerMessage = "HTTP STATUS: " + clientResponse.getStatus();
				}
	
			} catch (ClientHandlerException e) {
				e.printStackTrace();
				loggerMessage = e.toString();
			} catch (Exception e) {
				e.printStackTrace();
				loggerMessage = e.toString();
			}
		
	}

		wsLogger.saveRegistroLog(new LogWs(data.getCredencial(), loggerEvent, loggerMessage, loggerParams));

		return resultadoVenta;
	}

	/* -------------------------------------------------- */
	
	public int generarNroConexion(String url, CredencialWrap credencial) {
		return 1;
	}

	public int generarNroConexion(InstanciaWS inst, CredencialWrap credencial) {
		return 1;
	}

	// {!} ANULAR (sin penalidad) o DEVOLVER (con penalidad)

	public double getPorcentajeDevolucionCanje(AnulacionDevolucionWsDto param)
			throws WebServiceException, RuntimeException {

		TipoEventoLog loggerEvent = TipoEventoLog.porcentajeDevolucionCanje;

		double descuento = 0;
		String domain = param.getCredencial().getUrl();
		String tin = param.getNumeroBoleto();

		String request = domain + "cancellationdata?tin=" + tin;

		loggerParams = "Tin: " + tin;

		try {
			ClientResponse clientResponse = JerseyClient_byOAuth.getJerseyWebResourceByOAuth(request);

			if (clientResponse.getStatus() == HttpStatus.SC_OK) {

				String response = clientResponse.getEntity(String.class);

				CancellationData cancellationData = gson.fromJson(response, CancellationData.class);

				loggerMessage = (cancellationData != null) ? "OK"
						: "NULL RESPONSE. HTTP STATUS: " + clientResponse.getStatus();

				String seatName = param.getNroAsiento();

				double numerador = cancellationData.getFares().get(seatName).doubleValue();
				double denominador = cancellationData.getCancellationCharges().get(seatName).doubleValue();

				descuento = numerador / (numerador - denominador);

			} else if (clientResponse.getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
				loggerMessage = "ERROR: " + clientResponse.getStatus();
			} else {
				loggerMessage = "HTTP STATUS: " + clientResponse.getStatus();
			}

		} catch (ClientHandlerException e) {
			e.printStackTrace();
			loggerMessage = e.toString();
		} catch (RuntimeException e) {
			e.printStackTrace();
			loggerMessage = e.toString();
		} catch (Exception e) {
			e.printStackTrace();
			loggerMessage = e.toString();
		}

		wsLogger.saveRegistroLog(new LogWs(param.getCredencial(), loggerEvent, loggerMessage, loggerParams));

		return descuento;
	}

	public boolean anularBoletoVendido(AnulacionDevolucionWsDto param) throws WebServiceException, RuntimeException {

		TipoEventoLog loggerEvent = TipoEventoLog.anularBoletoVendido;

		CancellationRequest cancellationRequest = new CancellationRequest();

		cancellationRequest.setTin(param.getTransaccion());
		cancellationRequest.addSeatToCancel(param.getNroAsiento());

		String postString = gson.toJson(cancellationRequest, CancellationRequest.class);

		loggerParams = "Request Body: " + postString;

		String domain = param.getCredencial().getUrl();
		String request = domain + "cancelticket";

		try {
			ClientResponse clientResponse = JerseyClient_byOAuth.postJerseyWebResourceByOAuth(request, postString);
			String response = clientResponse.getEntity(String.class);

			if (clientResponse.getStatus() == HttpStatus.SC_OK) {

				CancellationResponse cancellationResponse = gson.fromJson(response, CancellationResponse.class);
				loggerMessage = cancellationResponse != null ? "OK"
						: "NULL RESPONSE. HTTP STATUS: " + clientResponse.getStatus();
			} else if (clientResponse.getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
				loggerMessage = "ERROR: " + clientResponse.getStatus();
			} else {
				loggerMessage = "HTTP STATUS: " + clientResponse.getStatus();
			}

			/*
			 * { "tin": "{{tin}}", "seatsToCancel": [ "{{seatName1}}" ], "notes": "" }
			 * CancellationRequest { tin (string, optional), seatsToCancel (Array[string],
			 * optional), notes (string, optional) }
			 */
		} catch (ClientHandlerException e) {
			e.printStackTrace();
			loggerMessage = e.toString();
		} catch (RuntimeException e) {
			e.printStackTrace();
			loggerMessage = e.toString();
		} catch (Exception e) {
			e.printStackTrace();
			loggerMessage = e.toString();
		}

		wsLogger.saveRegistroLog(new LogWs(param.getCredencial(), loggerEvent, loggerMessage, loggerParams));
		return loggerMessage.equals("OK");
	}

	/* -------------------------------------------------- */

	public String devolverBoletoVendido(AnulacionDevolucionWsDto param) throws WebServiceException, RuntimeException {
		// TODO Auto-generated method stub
		return null;
	}

	/* -------------------------------------------------- */

	public List<ParadaWS> ObtenerParadasHomologadas(String url) {
		// TODO Auto-generated method stub
		return null;
	}

	public BloqueoButaca desbloquearButacas(ServicioWrap servicio, String butacas, CredencialWrap credencial) {
		// TODO Auto-generated method stub
		return null;
	}


	public int nextBoletoLibre(String codigo, Empresa empresa, CredencialWrap credencial) throws WebServiceException {
		// TODO Auto-generated method stub
		return 0;
	}

	public List<ParadaWS> getRecorridoServicio(ServicioWrap servicio, CredencialWrap credencial)
			throws WebServiceException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<OpcionesPago> getOpcionesDePagoHabilitadas(DataConsultarMediosDePagoDTO data) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean liberarReserva(ReservaProsys reserva) {
		// TODO Auto-generated method stub
		return false;
	}

	public List<Elements> getFinanciacionPorTarjeta(FinanciacionWrap financiacion, ServicioWrap servicioIda,
			boolean ida, RegistroDeVenta venta) {
		// TODO Auto-generated method stub
		return null;
	}

	public ServicioWrap getStages(ServicioWrap serviceWrap, CredencialWrap credential) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<DataTaquilla> getMultiTaquilla(ServicioWrap serviceWrap, CredencialWrap credential) {
		// TODO Auto-generated method stub
		return null;
	}

	public BloqueoButaca bloquearMultiButaca(ServicioWrap servicio, Map<Integer, String> butacas,
			CredencialWrap credencial) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<ServicioWrap> getServicios(IParada origen, IParada destino, Date fecha, Date fechaR, String conexion,
			String url, CredencialWrap credencial, String tiposPasajeros) {
		// TODO Auto-generated method stub
		return null;
	}

}
