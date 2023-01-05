import React from "react";
import Autosuggest from "react-autosuggest";
import "./AutosuggestTheme.css";
import { config } from "./Config.js";

/**
 * Resource for search and autosuggest
 */
export default class Search extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      value: "",
      suggestions: [],
      fetchError: false,
    };
  }

  fetchSuggestions(value) {
    if (value.length <= 3) {
      var suggestions = [];
      this.setState({
        suggestions: suggestions,
      });
    } else {
      fetch("/api/v2/resources/search?idPrefix=" + value)
        .then((res) => {
          if (res.ok) {
            return res.json();
          } else {
            throw new Error("Error fetching search data");
          }
        })
        .then((result) => {
          var suggestions = [];
          var suggestionMap = {};
          for (var i in result) {
            var entry = result[i];
            if (!suggestionMap[entry.type]) {
              suggestionMap[entry.type] = [];
            }
            suggestionMap[entry.type].push(entry.id);
          }
          Object.keys(suggestionMap).forEach((key) => {
            var v = key.split(".");
            suggestions.push({
              title: v[v.length - 1],
              elements: suggestionMap[key],
            });
          });
          this.setState({
            suggestions: suggestions,
          });
        })
        .catch((error) => {
          console.log(error);
          this.setState({
            fetchError: true,
          });
        });
    }
  }

  escapeRegexCharacters(str) {
    return str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  }

  getSuggestionValue(suggestion) {
    return suggestion;
  }

  renderSuggestion(suggestion) {
    return <span>{suggestion}</span>;
  }

  renderSectionTitle(section) {
    return <strong>{section.title}</strong>;
  }

  getSectionSuggestions(section) {
    return section.elements;
  }

  onChange = (event, { newValue, method }) => {
    if (newValue === "") {
      this.setState({
        suggestedSections: [],
      });
    }
    this.setState({
      value: newValue,
    });
    if (this.props.onChange) {
      this.props.onChange(newValue);
    }
  };

  onSuggestionsFetchRequested = ({ value, reason }) => {
    this.fetchSuggestions(value);
  };

  onSuggestionsClearRequested = () => {
    this.setState({
      suggestions: [],
    });
  };

  onSuggestionSelected = (
    event,
    { suggestion, suggestionValue, suggestionIndex, sectionIndex, method }
  ) => {
    this.props.handleSubmit(suggestion);
    // this.props.handleSubmit(
    //   suggestionValue,
    //   suggestion.id,
    //   this.state.suggestions[sectionIndex]["title"]
    // );
  };

  render() {
    const { value, suggestions } = this.state;
    const inputProps = {
      placeholder: "Resource Search",
      value,
      onChange: this.onChange,
    };

    return (
      <Autosuggest
        multiSection={true}
        suggestions={suggestions}
        onSuggestionsFetchRequested={this.onSuggestionsFetchRequested}
        onSuggestionsClearRequested={this.onSuggestionsClearRequested}
        getSuggestionValue={this.getSuggestionValue}
        renderSuggestion={this.renderSuggestion}
        renderSectionTitle={this.renderSectionTitle}
        getSectionSuggestions={this.getSectionSuggestions}
        onSuggestionSelected={this.onSuggestionSelected}
        inputProps={inputProps}
      />
    );
  }
}
