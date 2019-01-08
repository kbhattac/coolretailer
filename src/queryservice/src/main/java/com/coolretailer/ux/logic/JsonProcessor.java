package com.coolretailer.ux.logic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("JSON")
public class JsonProcessor implements ApplicationRunner {

	public void run(ApplicationArguments args) throws Exception {
		if (args.containsOption("input.json")) {
			toNdJson(args.getOptionValues("input.json").get(0));
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(JsonProcessor.class);

	private void toNdJson(String input) throws Exception {
		String output = input.replaceAll(".json", ".nd.json");
		File outFile = new File(output);

		if (outFile.exists()) {
			LOGGER.info("Deleting existing file: " + output);
			outFile.delete();
		}
		JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(input), "UTF-8"));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output)));

		reader.beginArray();
		JsonParser parser = new JsonParser();
		long count = 0;
		while (reader.hasNext()) {
			JsonElement element = parser.parse(reader);
			writer.write(element.toString());
			writer.append('\n');
			writer.flush();
			count++;
		}
		LOGGER.info("Processed " + count + " records.");
		writer.close();
		reader.close();
	}

}
