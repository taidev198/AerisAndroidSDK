package com.example.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;

import com.example.db.MyPlacesDb;
import com.hamweather.aeris.communication.Action;
import com.hamweather.aeris.communication.AerisCallback;
import com.hamweather.aeris.communication.AerisCommunicationTask;
import com.hamweather.aeris.communication.AerisProgressListener;
import com.hamweather.aeris.communication.AerisRequest;
import com.hamweather.aeris.communication.BatchBuilder;
import com.hamweather.aeris.communication.BatchCallback;
import com.hamweather.aeris.communication.BatchCommunicationTask;
import com.hamweather.aeris.communication.Endpoint;
import com.hamweather.aeris.communication.EndpointType;
import com.hamweather.aeris.communication.fields.Fields;
import com.hamweather.aeris.communication.fields.ForecastsFields;
import com.hamweather.aeris.communication.fields.ObservationFields;
import com.hamweather.aeris.communication.parameter.FieldsParameter;
import com.hamweather.aeris.communication.parameter.FilterParameter;
import com.hamweather.aeris.communication.parameter.FromParameter;
import com.hamweather.aeris.communication.parameter.LimitParameter;
import com.hamweather.aeris.communication.parameter.PLimitParameter;
import com.hamweather.aeris.communication.parameter.PlaceParameter;
import com.hamweather.aeris.communication.parameter.ToParameter;
import com.hamweather.aeris.model.AerisBatchResponse;
import com.hamweather.aeris.model.AerisResponse;

public class HeadlessFragment extends Fragment implements AerisCallback,
		BatchCallback {

	public interface HeadlessObserver {
		void notifyDataChanged();
	}

	protected static final String DETAILED_OBSERVATION = "detailed_observation";
	protected static final String WEEKEND = "weekend_forecasts";
	protected static final String EXT_FORECAST = "extended_forecasts";
	protected static final String NEARBY_OBS = "nearby_observations";
	protected static final String RECENT_OBS = "recent_observations";
	protected static final String OVERVIEW = "weather_overview";

	private int currentFragment = 0;
	private static final long TEN_MINUTES = 1000 * 60 * 10;
	private Map<String, Object> map = new HashMap<String, Object>();
	private Map<String, Long> timeMap = new HashMap<String, Long>();
	private static List<HeadlessObserver> observers = new ArrayList<HeadlessObserver>();

	public static HeadlessFragment getFragment(Activity activity) {
		// create headless fragment
		FragmentManager fragmentManager = activity.getFragmentManager();
		HeadlessFragment fragment = (HeadlessFragment) fragmentManager
				.findFragmentByTag("Headless");
		if (fragment == null) {
			fragment = new HeadlessFragment();
			fragmentManager.beginTransaction().add(fragment, "Headless")
					.commit();
		}
		return fragment;
	}

	public static void addObserver(HeadlessObserver observer) {
		observers.add(observer);
	}

	public static void removeObserver(HeadlessObserver observer) {
		observers.remove(observer);
	}

	public void storeResponse(String key, Object object) {
		map.put(key, object);
		timeMap.put(key, System.currentTimeMillis());
	}

	public Object getResponse(String key) {
		if (timeMap.get(key) == null) {
			return null;
		} else {
			if (System.currentTimeMillis() - timeMap.get(key) > TEN_MINUTES) {
				return null;
			} else {
				return map.get(key);
			}
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// The heart and mind of headless fragment is below line. It will keep
		// the fragment alive during configuration change when activities and
		// //subsequent fragments are "put to death" and recreated
		setRetainInstance(true);
	}

	public void performWeatherOverview(AerisProgressListener listener) {
		BatchBuilder builder = new BatchBuilder();
		builder.addGlobalParameter(this.getPlaceParameter());
		builder.addEndpoint(new Endpoint(EndpointType.OBSERVATIONS,
				Action.CLOSEST).addParameters(FieldsParameter.initWith("ob")));
		builder.addEndpoint(new Endpoint(EndpointType.PLACES, Action.CLOSEST)
				.addParameters(FieldsParameter.initWith("place")));
		builder.addEndpoint(new Endpoint(EndpointType.FORECASTS, Action.CLOSEST)
				.addParameters(FieldsParameter.initWith(
						ForecastsFields.WEATHER_PRIMARY,
						ForecastsFields.MAX_TEMP_F, ForecastsFields.ICON,
						ForecastsFields.DATETIME_ISO,
						ForecastsFields.MIN_TEMP_F)));
		AerisRequest request = builder.build();
		BatchCommunicationTask task = new BatchCommunicationTask(getActivity(),
				this, request);
		if (listener != null)
			task.withProgress(listener);
		task.execute();
	}

	public void performNearbyObs(AerisProgressListener listener) {

		AerisRequest request = new AerisRequest(new Endpoint(
				EndpointType.OBSERVATIONS), Action.CLOSEST,
				getPlaceParameter(), FieldsParameter.initWith(
						ObservationFields.TEMP_C, ObservationFields.TEMP_F,
						ObservationFields.ICON,
						ObservationFields.WEATHER_SHORT, Fields.PLACE,
						ObservationFields.DATETIME), new LimitParameter(10));
		AerisCommunicationTask task = new AerisCommunicationTask(getActivity(),
				this, request);
		if (listener != null) {
			task.withProgress(listener);
		}
		task.execute();
	}

	public void performRecentsObs(AerisProgressListener listener) {
		AerisRequest request = new AerisRequest(new Endpoint(
				EndpointType.OBSERVATIONS_RECENT), Action.CLOSEST,
				getPlaceParameter(), new PLimitParameter(10));
		AerisCommunicationTask task = new AerisCommunicationTask(getActivity(),
				this, request);
		if (listener != null) {
			task.withProgress(listener);
		}
		task.execute();
	}

	public void performExtForecast(AerisProgressListener listener) {
		AerisRequest request = new AerisRequest(new Endpoint(
				EndpointType.FORECASTS), Action.CLOSEST, getPlaceParameter(),
				FieldsParameter.initWith(Fields.INTERVAL,
						ForecastsFields.WEATHER_PRIMARY,
						ForecastsFields.MAX_TEMP_F, ForecastsFields.ICON,
						ForecastsFields.DATETIME_ISO,
						ForecastsFields.MIN_TEMP_F), new FilterParameter("7"),
				new LimitParameter(10));
		AerisCommunicationTask task = new AerisCommunicationTask(getActivity(),
				this, request);
		if (listener != null) {
			task.withProgress(listener);
		}
		task.execute();
	}

	public void performWeekendForecast(AerisProgressListener listener) {
		AerisRequest request = new AerisRequest(new Endpoint(
				EndpointType.FORECASTS), Action.CLOSEST, getPlaceParameter(),
				new FilterParameter("daynight"), new FromParameter("friday"),
				new ToParameter("+3days"));
		AerisCommunicationTask task = new AerisCommunicationTask(getActivity(),
				this, request);
		task.withProgress(listener);
		task.execute();
	}

	public void performDetailedObservation(AerisProgressListener listener) {
		BatchBuilder builder = new BatchBuilder();
		builder.addGlobalParameter(getPlaceParameter());
		builder.addEndpoint(new Endpoint(EndpointType.OBSERVATIONS,
				Action.CLOSEST).addParameters(FieldsParameter.initWith("ob")));
		builder.addEndpoint(new Endpoint(EndpointType.PLACES, Action.CLOSEST)
				.addParameters(FieldsParameter.initWith("place")));
		builder.addEndpoint(new Endpoint(EndpointType.FORECASTS, Action.CLOSEST)
				.addParameters(new FilterParameter("daynight"),
						new PLimitParameter(2)));
		builder.addEndpoint(new Endpoint(EndpointType.FORECASTS, Action.CLOSEST)
				.addParameters(new FilterParameter("3hr"), new PLimitParameter(
						8), FieldsParameter.initWith(ForecastsFields.TEMP_F,
						ForecastsFields.TEMP_C, ForecastsFields.ICON,
						ForecastsFields.DATETIME_ISO, Fields.INTERVAL)));
		AerisRequest request = builder.build();
		BatchCommunicationTask task = new BatchCommunicationTask(getActivity(),
				this, request);
		if (listener != null) {
			task.withProgress(listener);
		}
		task.execute();
	}

	public void performCall(AerisRequest request, AerisCallback callback,
			BatchCallback batchCallback, AerisProgressListener listener) {
		if (callback != null) {
			AerisCommunicationTask task = new AerisCommunicationTask(
					getActivity(), callback, request);

			if (listener != null) {
				task.withProgress(listener);
			}
			task.execute();
		} else if (batchCallback != null) {
			BatchCommunicationTask task = new BatchCommunicationTask(
					getActivity(), batchCallback, request);

			if (listener != null) {
				task.withProgress(listener);
			}
			task.execute();
		}

	}

	private void notifyObservers() {
		for (HeadlessObserver observer : observers) {
			observer.notifyDataChanged();
		}
	}

	@Override
	public void onBatchResponse(AerisBatchResponse response) {
		if (response.isSuccessful() && response.getError() == null) {
			if (response.responses.size() == 3) {
				storeResponse(OVERVIEW, response);
			} else if (response.responses.size() == 4) {
				storeResponse(DETAILED_OBSERVATION, response);
			}
		}
		notifyObservers();
	}

	@Override
	public void onResult(EndpointType endpoint, AerisResponse response) {
		if (response.isSuccessfulWithResponses()) {
			if (endpoint == EndpointType.FORECASTS) {
				if ("daynight".equals(response.getFirstResponse().interval)) {
					storeResponse(WEEKEND, response);
				} else if ("day".equals(response.getFirstResponse().interval)) {
					storeResponse(EXT_FORECAST, response);
				}
			} else if (endpoint == EndpointType.OBSERVATIONS_RECENT) {
				storeResponse(RECENT_OBS, response);
			} else if (endpoint == EndpointType.OBSERVATIONS) {
				storeResponse(NEARBY_OBS, response);
			}
			notifyObservers();
		}
	}

	public int getCurrentFragment() {
		return currentFragment;
	}

	public void setCurrentFragment(int currentFragment) {
		this.currentFragment = currentFragment;
	}

	public void clearStored() {
		map.clear();
		timeMap.clear();
	}

	private PlaceParameter getPlaceParameter() {
		MyPlacesDb db = new MyPlacesDb(getActivity());
		PlaceParameter place = db.getMyPlaceParameter();
		db.close();
		if (place == null) {
			place = new PlaceParameter(getActivity());
		}
		return place;
	}

}
