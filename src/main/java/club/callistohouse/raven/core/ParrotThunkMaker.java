package club.callistohouse.raven.core;

import club.callistohouse.raven.remote.ParrotTransform;
import club.callistohouse.session.EncoderThunkMaker;
import club.callistohouse.session.SessionIdentity;

public class ParrotThunkMaker extends EncoderThunkMaker {

	private RavenServer tuner;

	public ParrotThunkMaker(RavenServer aTuner) { super("parrot", null); tuner = aTuner; }

	public ParrotTransform makeThunkOnFarKey(SessionIdentity farKey) {
		ParrotTransform thunk = new ParrotTransform(tuner.getScopeForFarKey(farKey));
		return thunk;
	}
}
