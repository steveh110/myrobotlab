package org.myrobotlab.codec;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Encoder;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class ArduinoBindingsGenerator {

	public transient final static Logger log = LoggerFactory.getLogger(ArduinoBindingsGenerator.class);

	static TreeMap<String, Method> sorted = new TreeMap<String, Method>();

	static StringBuilder ino = new StringBuilder("///// INO GENERATED DEFINITION BEGIN //////\n");

	static StringBuilder javaDefines = new StringBuilder("\t///// java ByteToMethod generated definition - DO NOT MODIFY - Begin //////\n");
	static StringBuilder javaBindingsInit = new StringBuilder();

	public static void createBindingsFor(String key, int index) {

		Method method = sorted.get(key);
		StringBuilder msb = new StringBuilder(method.getName());
		Class<?>[] params = method.getParameterTypes();
		for (int j = 0; j < params.length; ++j) {
			msb.append(String.format(" %s", params[j].getSimpleName()/*
																	 * .toString(
																	 * )
																	 */));
		}

		String methodSignatureComment = String.format("// {%s} \n", msb.toString());

		ino.append(methodSignatureComment);
		String underscore = Encoder.toUnderScore(method.getName());
		ino.append(String.format("#define %s\t\t%d\n\n", underscore, index));

		javaDefines.append(String.format("\t%s", methodSignatureComment));
		javaDefines.append(String.format("\tpublic final static int %s =\t\t%d;\n\n", underscore, index));
		javaBindingsInit.append(String.format("\t\tbyteToMethod.put(%s,\"%s\");\n", underscore, method.getName()));
		javaBindingsInit.append(String.format("\t\tmethodToByte.put(\"%s\",%s);\n\n", method.getName(), underscore));
		// javaByteToMethod.append(String.format("\tmethodToByte.put(%d,\"%s\");\n\n",
		// index, method.getName()));

	}

	public static void generateDefinitions() throws IOException {

		HashMap<String, String> snr = new HashMap<String, String>();

		Arduino arduino = (Arduino) Runtime.start("arduino", "Arduino");
		Method[] m = arduino.getDeclaredMethods();
		for (int i = 0; i < m.length; ++i) {
			// String signature = Encoder.getMethodSignature(m[i]);
			Method method = m[i];
			StringBuilder msb = new StringBuilder(m[i].getName());
			Class<?>[] params = method.getParameterTypes();
			for (int j = 0; j < params.length; ++j) {
				msb.append(String.format(" %s", params[j].toString()));
			}

			// log.info(String.format("[%s]", msb.toString())); // hmmm
			// "class someclass" :(
			// get rid of overloads by taking the method with maximum
			// complexity(ish) :)
			if (sorted.containsKey(method.getName())) {
				// overloaded - who has more parameters
				Method complex = (sorted.get(method.getName()).getParameterTypes().length < method.getParameterTypes().length) ? method : sorted.get(method.getName());
				sorted.put(method.getName(), complex);
			} else {
				sorted.put(method.getName(), method);
			}
		}

		HashSet<String> exclude = new HashSet<String>();

		// filter out methods which do not get relayed
		// to the Arduino
		exclude.add("addCustomMsgListener");
		exclude.add("connect");
		exclude.add("disconnect");
		exclude.add("getDescription");
		exclude.add("createPinList");
		exclude.add("getBoardType");
		exclude.add("getCategories");
		exclude.add("getPeers");
		exclude.add("getPinList");
		exclude.add("getSerial");
		exclude.add("getStepperData");// WTF
		exclude.add("isConnected");
		exclude.add("main");
		exclude.add("onByte");
		exclude.add("onCustomMsg");
		exclude.add("releaseService");
		exclude.add("sendMsg");
		exclude.add("setBoardType");
		exclude.add("startService");
		exclude.add("test");

		// additionally force getversion and publishMRLCommError
		// so that mis-matches of version are quickly reported...
		exclude.add("getVersion");
		exclude.add("publishVersion");
		exclude.add("publishMRLCommError");
		// except.add("E");

		// getter & setters
		exclude.add("setRXFormatter");
		exclude.add("setTXFormatter");
		exclude.add("getRXFormatter");
		exclude.add("getTXFormatter");
		exclude.add("getRXCodecKey");
		exclude.add("getTXCodecKey");
		exclude.add("connectVirtualUART");	
		exclude.add("getSketch");	
		exclude.add("onConnect");	
		exclude.add("onDisconnect");	
		exclude.add("setBoard");	
		exclude.add("setBoard");	
		exclude.add("setSketch");	

		
		int index = 0;

		++index;
		createBindingsFor("publishMRLCommError", index);
		++index;
		createBindingsFor("getVersion", index);
		++index;
		createBindingsFor("publishVersion", index);
		++index;

		for (String key : sorted.keySet()) {
			if (exclude.contains(key)) {
				continue;
			}

			createBindingsFor(key, index);

			++index;
			// log.info(); // hmmm "class someclass" :(
		}

		ino.append("///// INO GENERATED DEFINITION END //////\n");

		snr.put("<%=java.defines%>", javaDefines.toString());
		snr.put("<%=java.bindings.init%>", javaBindingsInit.toString());
		snr.put("<%=mrlcomm.defines%>", ino.toString());

		log.info(ino.toString());
		log.info(javaBindingsInit.toString());

		// file template filtering
		String ArduinoMsgCodec = FileIO.resourceToString("Arduino/ArduinoMsgCodec.txt");
		String MRLComm = FileIO.resourceToString("Arduino/MRLComm.txt");

		for (String key : snr.keySet()) {
			ArduinoMsgCodec = ArduinoMsgCodec.replace(key, snr.get(key));
			log.info(snr.get(key));
			MRLComm = MRLComm.replace(key, snr.get(key));
			// MRLComm = MRLComm.replaceAll(key, snr.get(key));
		}

		FileIO.stringToFile("ArduinoMsgCodec.out.txt", ArduinoMsgCodec);
		FileIO.stringToFile("MRLComm.out.txt", MRLComm);

		// String ret = String.format("%s\n\n%s", ino.toString(),
		// java.toString());
		// log.info(ret);
		// to define (upper with underscore)

		Runtime.exit();
	}

	public static void main(String[] args) {
		try {

			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);

			String t = "testMethod";
			// camelback to underscore
			/*
			 * String regex = "[A-Z\\d]"; String replacement = "$1_";
			 * log.info(t.replaceAll(regex, replacement)); log.info(t);
			 */

			log.info(Encoder.toUnderScore(t));
			log.info(Encoder.toCamelCase(Encoder.toUnderScore(t)));

			ArduinoBindingsGenerator.generateDefinitions();

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

}
