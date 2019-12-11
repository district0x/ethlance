import {ApolloProvider, useQuery, useMutation} from '@apollo/react-hooks';
import {InMemoryCache, defaultDataIdFromObject} from 'apollo-cache-inmemory';
import {ApolloClient} from 'apollo-client';
import {ApolloLink} from 'apollo-link';
import {setContext} from 'apollo-link-context';
// import {createUploadLink} from 'apollo-upload-client';
import {HttpLink} from 'apollo-link-http';
import * as gql from "graphql-tag";

import React from 'react';
import ReactDOM from 'react-dom';

window.React = React;
window.ReactDOM = ReactDOM;

window.ApolloProvider = ApolloProvider;
window.useQuery = useQuery;
window.useMutation = useMutation;
window.InMemoryCache = InMemoryCache;
window.defaultDataIdFromObject = defaultDataIdFromObject;
window.ApolloClient = ApolloClient;
window.ApolloLink = ApolloLink;
window.setContext = setContext;
window.HttpLink = HttpLink;
