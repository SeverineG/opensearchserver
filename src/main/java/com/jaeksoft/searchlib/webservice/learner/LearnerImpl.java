/**   
 * License Agreement for OpenSearchServer
 *
 * Copyright (C) 2013 Emmanuel Keller / Jaeksoft
 * 
 * http://www.open-search-server.com
 * 
 * This file is part of OpenSearchServer.
 *
 * OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OpenSearchServer. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.jaeksoft.searchlib.webservice.learner;

import java.io.IOException;

import com.jaeksoft.searchlib.Client;
import com.jaeksoft.searchlib.ClientFactory;
import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.learning.Learner;
import com.jaeksoft.searchlib.learning.LearnerManager;
import com.jaeksoft.searchlib.user.Role;
import com.jaeksoft.searchlib.webservice.CommonResult;
import com.jaeksoft.searchlib.webservice.CommonServices;

public class LearnerImpl extends CommonServices implements SoapLearner,
		RestLearner {

	private Learner getLearner(Client client, String name)
			throws SearchLibException {
		LearnerManager manager = client.getLearnerManager();
		Learner learner = manager.get(name);
		if (learner == null)
			throw new CommonServiceException("Learner " + name + " not found");
		return learner;
	}

	@Override
	public LearnerResult classify(String index_name, String login, String key,
			String learner_name, int max_rank, double min_score, String text) {
		try {
			Client client = getLoggedClient(index_name, login, key,
					Role.INDEX_QUERY);
			ClientFactory.INSTANCE.properties.checkApi();
			Learner learner = getLearner(client, learner_name);
			return new LearnerResult(learner.classify(client, text, max_rank,
					min_score));
		} catch (SearchLibException e) {
			throw new CommonServiceException(e);
		} catch (IOException e) {
			throw new CommonServiceException(e);
		} catch (InterruptedException e) {
			throw new CommonServiceException(e);
		}
	}

	@Override
	public LearnerResult classifyPost(String index_name, String login,
			String key, String learner_name, int max_rank, double min_score,
			String text) {
		return classify(index_name, login, key, learner_name, max_rank,
				min_score, text);
	}

	@Override
	public CommonResult learn(String index, String login, String key,
			String learner_name) {
		try {
			Client client = getLoggedClient(index, login, key,
					Role.INDEX_UPDATE);
			ClientFactory.INSTANCE.properties.checkApi();
			Learner learner = getLearner(client, learner_name);
			learner.reset(client);
			CommonResult result = new CommonResult(true, null);
			learner.learn(client, result);
			return result;
		} catch (SearchLibException e) {
			throw new CommonServiceException(e);
		} catch (IOException e) {
			throw new CommonServiceException(e);
		} catch (InterruptedException e) {
			throw new CommonServiceException(e);
		}
	}

}
