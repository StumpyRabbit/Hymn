package club.callistohouse.raven.remote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import club.callistohouse.raven.scope.Scope;
import club.callistohouse.session.EncoderThunk;

public class ParrotTransform extends EncoderThunk {

	private Scope scope;
	
	public ParrotTransform(Scope aScope) { super("parrot"); scope = aScope; }

	@SuppressWarnings("resource")
	@Override
	public Object serializeThunk(Object chunk) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		new RavenOutputStream(baos, scope).writeObject(chunk);
		return baos.toByteArray();
	}

	@SuppressWarnings("resource")
	@Override
	public Object materializeThunk(Object chunk) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bais = new ByteArrayInputStream((byte[]) chunk);
		return (RavenMessage)new RavenInputStream(bais, scope).readObject();
	}
}
